package au.com.getkempt.common.persistence;

import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.pagination.CursorCodec;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.pagination.PageInfo;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.tenant.TenantContextHolder;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Base data-access class for every tenant-scoped aggregate. This is GetKempt's
 * answer to the Postgres Row-Level-Security the spec assumes: because MongoDB has
 * no RLS, EVERY read and write goes through here and is unconditionally constrained
 * to {@code shopId = currentTenant} (resolved from the JWT, never user input) and,
 * for reads, {@code deletedAt = null}. Domain repositories extend this rather than
 * hand-writing {@code Criteria}, so a query can never "forget" the tenant filter.
 *
 * <p>List queries use keyset (cursor) pagination: we sort by the requested field
 * plus {@code _id} as a stable tiebreak, and the cursor carries the boundary
 * document's id. The boundary's sort value is re-read server-side, keeping the
 * cursor opaque and correct for any field type.
 */
public abstract class TenantRepository<T extends BaseDocument> {

    protected final MongoTemplate template;
    private final Class<T> type;
    private final IdPrefix idPrefix;

    protected TenantRepository(MongoTemplate template, Class<T> type, IdPrefix idPrefix) {
        this.template = template;
        this.type = type;
        this.idPrefix = idPrefix;
    }

    // ── writes ────────────────────────────────────────────────────────────────

    public T insert(T doc) {
        if (doc.getId() == null) {
            doc.setId(IdGenerator.generate(idPrefix));
        }
        doc.setShopId(TenantContextHolder.requireShopId());
        doc.setDeletedAt(null);
        return template.insert(doc);
    }

    /** Upsert-style save. The tenant is re-stamped so ownership can never be reassigned. */
    public T save(T doc) {
        doc.setShopId(TenantContextHolder.requireShopId());
        return template.save(doc);
    }

    public void softDelete(T doc) {
        doc.setDeletedAt(Instant.now());
        save(doc);
    }

    // ── reads ─────────────────────────────────────────────────────────────────

    public Optional<T> findById(String id) {
        return Optional.ofNullable(template.findOne(scopedQuery(Criteria.where("_id").is(id)), type));
    }

    /** Returns the entity or throws 404 — note: cross-tenant ids resolve to 404, never 403. */
    public T requireById(String id) {
        return findById(id).orElseThrow(() -> NotFoundException.of(type.getSimpleName(), id));
    }

    public boolean existsById(String id) {
        return template.exists(scopedQuery(Criteria.where("_id").is(id)), type);
    }

    public long count(Criteria filter) {
        return template.count(scopedQuery(filter), type);
    }

    /**
     * Keyset-paginated, tenant-scoped, soft-delete-aware list.
     *
     * @param filter caller's business filter (must NOT reference shopId/deletedAt); may be null
     * @param sort   validated sort instruction
     * @param page   cursor + limit
     */
    public CursorPage<T> list(Criteria filter, SortRequest sort, CursorPageRequest page) {
        // Compose ONE root criteria. Callers' filters and the seek predicate are
        // typically key-less ($and / $or roots); Query.addCriteria rejects a second
        // key-less criteria, so stacking them with addCriteria fails on page 2+.
        List<Criteria> parts = new java.util.ArrayList<>();
        parts.add(tenantScope());
        if (filter != null) {
            parts.add(filter);
        }
        if (page.hasCursor()) {
            Criteria seek = seekCriteria(sort, page.boundaryId());
            if (seek != null) {
                parts.add(seek);
            }
        }
        Query query = new Query(new Criteria().andOperator(parts.toArray(Criteria[]::new)));

        Sort.Direction direction = sort.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, sort.field()).and(Sort.by(direction, "_id")));
        query.limit(page.limit() + 1);

        List<T> rows = template.find(query, type);
        boolean hasNextPage = rows.size() > page.limit();
        if (hasNextPage) {
            rows = new java.util.ArrayList<>(rows.subList(0, page.limit()));
        }
        String nextCursor = (hasNextPage && !rows.isEmpty())
                ? CursorCodec.encode(rows.get(rows.size() - 1).getId())
                : null;

        return new CursorPage<>(rows, PageInfo.of(nextCursor, hasNextPage));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    protected Criteria tenantScope() {
        return Criteria.where("shopId").is(TenantContextHolder.requireShopId()).and("deletedAt").is(null);
    }

    /**
     * Tenant scope AND the caller's filter, composed as a single {@code $and} root so
     * key-less filters (e.g. {@code new Criteria().andOperator(...)}) never collide.
     */
    protected Query scopedQuery(Criteria extra) {
        if (extra == null) {
            return new Query(tenantScope());
        }
        return new Query(new Criteria().andOperator(tenantScope(), extra));
    }

    /**
     * Keyset "seek" predicate for the page after {@code boundaryId}, or {@code null}
     * when the boundary document no longer exists (stale cursor degrades to an
     * unbounded page rather than failing).
     */
    private Criteria seekCriteria(SortRequest sort, String boundaryId) {
        // Re-read the boundary doc (tenant-scoped, ignoring soft-delete so a stale
        // cursor still paginates) to obtain its sort value.
        Query boundaryQuery = new Query(
                Criteria.where("_id").is(boundaryId)
                        .and("shopId").is(TenantContextHolder.requireShopId()));
        T boundary = template.findOne(boundaryQuery, type);
        if (boundary == null) {
            return null;
        }

        Object boundaryValue = new BeanWrapperImpl(boundary).getPropertyValue(sort.field());
        Criteria sameValueThenId = new Criteria().andOperator(
                Criteria.where(sort.field()).is(boundaryValue),
                sort.ascending()
                        ? Criteria.where("_id").gt(boundaryId)
                        : Criteria.where("_id").lt(boundaryId));
        Criteria beyondValue = sort.ascending()
                ? Criteria.where(sort.field()).gt(boundaryValue)
                : Criteria.where(sort.field()).lt(boundaryValue);

        return new Criteria().orOperator(beyondValue, sameValueThenId);
    }
}
