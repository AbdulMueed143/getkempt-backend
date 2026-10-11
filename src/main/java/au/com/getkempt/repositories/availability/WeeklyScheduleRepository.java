package au.com.getkempt.repositories.availability;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.availability.WeeklySchedule;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Tenant-scoped data access for {@link WeeklySchedule}. One document per staff
 * member, keyed deterministically by {@code id == staffId} (see the entity
 * javadoc), so lookup never needs an intermediate "find the schedule id" query.
 */
@Repository
public class WeeklyScheduleRepository extends TenantRepository<WeeklySchedule> {

    public WeeklyScheduleRepository(MongoTemplate template) {
        super(template, WeeklySchedule.class, IdPrefix.WEEKLY_SCHEDULE);
    }

    /** Fetch the staff member's schedule, creating an all-days-off default if none exists yet. */
    public WeeklySchedule getOrCreate(String staffId, String defaultTimezone) {
        return findById(staffId).orElseGet(() -> {
            WeeklySchedule schedule = new WeeklySchedule();
            schedule.setId(staffId); // deterministic key — see class javadoc
            schedule.setStaffId(staffId);
            schedule.setTimezone(defaultTimezone);
            schedule.setDays(WeeklySchedule.defaultDays());
            return insert(schedule);
        });
    }

}
