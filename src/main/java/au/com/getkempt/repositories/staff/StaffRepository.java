package au.com.getkempt.repositories.staff;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.StaffMember;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped data access for {@link StaffMember}. */
@Repository
public class StaffRepository extends TenantRepository<StaffMember> {

    public StaffRepository(MongoTemplate template) {
        super(template, StaffMember.class, IdPrefix.STAFF);
    }

    public Optional<StaffMember> findByUserId(String userId) {
        return Optional.ofNullable(
                template.findOne(scopedQuery(Criteria.where("userId").is(userId)), StaffMember.class));
    }

    /** Used during token mint (before {@link TenantContextHolder} is populated). */
    public Optional<StaffMember> findByShopIdAndUserId(String shopId, String userId) {
        Query q = new Query(Criteria.where("shopId").is(shopId)
                .and("userId").is(userId)
                .and("deletedAt").is(null));
        return Optional.ofNullable(template.findOne(q, StaffMember.class));
    }

    public Optional<StaffMember> findByEmailIgnoreCase(String email) {
        return Optional.ofNullable(template.findOne(
                scopedQuery(Criteria.where("email").is(email)), StaffMember.class));
    }

    /** Active members that can perform the given service (used by availability/booking later). */
    public List<StaffMember> findByServiceId(String serviceId) {
        return template.find(scopedQuery(Criteria.where("services").is(serviceId)), StaffMember.class);
    }

    /** Used by Bookings' package-slot-search, which qualifies staff via {@code Package.staffIds}. */
    public List<StaffMember> findByIds(Collection<String> ids) {
        return template.find(scopedQuery(Criteria.where("_id").in(ids)), StaffMember.class);
    }

    /** Used to resolve who gets bell-notified for shop-wide events (e.g. a new booking). */
    public List<StaffMember> findByRole(au.com.getkempt.common.security.StaffRole role) {
        return template.find(scopedQuery(Criteria.where("role").is(role)), StaffMember.class);
    }
}
