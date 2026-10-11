package au.com.getkempt.repositories.booking;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.booking.SlotLock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link SlotLock} — see the entity javadoc. */
@Repository
public class SlotLockRepository extends TenantRepository<SlotLock> {

    public SlotLockRepository(MongoTemplate template) {
        super(template, SlotLock.class, IdPrefix.SLOT_LOCK);
    }

    /** Releases every bucket a booking is holding — called on cancel/complete/no-show/reschedule. */
    public void releaseAll(String bookingId) {
        Query query = new Query(Criteria.where("shopId").is(TenantContextHolder.requireShopId())
                .and("bookingId").is(bookingId));
        template.remove(query, SlotLock.class);
    }
}
