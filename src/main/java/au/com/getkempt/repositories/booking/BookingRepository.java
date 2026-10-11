package au.com.getkempt.repositories.booking;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.booking.Booking;
import au.com.getkempt.enums.booking.BookingStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Tenant-scoped data access for {@link Booking}. */
@Repository
public class BookingRepository extends TenantRepository<Booking> {

    public BookingRepository(MongoTemplate template) {
        super(template, Booking.class, IdPrefix.BOOKING);
    }

    /**
     * Active (slot-holding) bookings for {@code staffId} whose [startAt, endAt)
     * overlaps [{@code startAt}, {@code endAt}) — the query-level half of conflict
     * detection (the transactional {@code SlotLock} rows are the race-safe half).
     */
    public List<Booking> findOverlapping(String staffId, Instant startAt, Instant endAt, String ignoreBookingId) {
        Criteria criteria = Criteria.where("staffId").is(staffId)
                .and("status").in(BookingStatus.ACTIVE)
                .and("startAt").lt(endAt)
                .and("endAt").gt(startAt);
        Query query = scopedQuery(criteria);
        if (ignoreBookingId != null) {
            query.addCriteria(Criteria.where("_id").ne(ignoreBookingId));
        }
        return template.find(query, Booking.class);
    }

    /** Used by {@code ClientService#merge} to move a soon-to-be-deleted client's booking history over. */
    public void reassignClient(String fromClientId, String toClientId) {
        Query query = scopedQuery(Criteria.where("clientId").is(fromClientId));
        template.updateMulti(query, org.springframework.data.mongodb.core.query.Update.update("clientId", toClientId), Booking.class);
    }

    public List<Booking> findForCalendar(Instant from, Instant to, Collection<String> staffIds) {
        Criteria criteria = Criteria.where("startAt").lt(to).and("endAt").gt(from);
        if (staffIds != null && !staffIds.isEmpty()) {
            criteria = criteria.and("staffId").in(staffIds);
        }
        Query query = scopedQuery(criteria).with(Sort.by(Sort.Direction.ASC, "startAt"));
        return template.find(query, Booking.class);
    }

    /** All active bookings for a staff member on a given date range — used by slot search. */
    public List<Booking> findActiveForStaffInRange(String staffId, Instant from, Instant to) {
        Criteria criteria = Criteria.where("staffId").is(staffId)
                .and("status").in(BookingStatus.ACTIVE)
                .and("startAt").lt(to)
                .and("endAt").gt(from);
        return template.find(scopedQuery(criteria), Booking.class);
    }

    /** Tenant-scoped escape hatch for ad-hoc reads (e.g. {@code DashboardService}'s aggregations). */
    public List<Booking> findMatching(Criteria filter) {
        return template.find(scopedQuery(filter), Booking.class);
    }

    public List<Booking> findMatching(Criteria filter, Sort sort, int limit) {
        Query query = scopedQuery(filter).with(sort).limit(limit);
        return template.find(query, Booking.class);
    }
}
