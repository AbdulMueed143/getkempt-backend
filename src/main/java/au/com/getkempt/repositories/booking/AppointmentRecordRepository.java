package au.com.getkempt.repositories.booking;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.booking.AppointmentRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Tenant-scoped data access for {@link AppointmentRecord}. */
@Repository
public class AppointmentRecordRepository extends TenantRepository<AppointmentRecord> {

    public AppointmentRecordRepository(MongoTemplate template) {
        super(template, AppointmentRecord.class, IdPrefix.APPOINTMENT);
    }

    public Optional<AppointmentRecord> findByBookingId(String bookingId) {
        return Optional.ofNullable(
                template.findOne(scopedQuery(Criteria.where("bookingId").is(bookingId)), AppointmentRecord.class));
    }
}
