package au.com.getkempt.repositories.availability;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.availability.AvailabilityOverride;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped data access for {@link AvailabilityOverride}. */
@Repository
public class AvailabilityOverrideRepository extends TenantRepository<AvailabilityOverride> {

    public AvailabilityOverrideRepository(MongoTemplate template) {
        super(template, AvailabilityOverride.class, IdPrefix.AVAILABILITY_OVERRIDE);
    }

    public Optional<AvailabilityOverride> findByStaffIdAndDate(String staffId, LocalDate date) {
        Query query = scopedQuery(Criteria.where("staffId").is(staffId).and("date").is(date));
        return Optional.ofNullable(template.findOne(query, AvailabilityOverride.class));
    }

    public List<AvailabilityOverride> findByStaffIdAndDateRange(String staffId, LocalDate from, LocalDate to) {
        Query query = scopedQuery(Criteria.where("staffId").is(staffId).and("date").gte(from).lte(to));
        return template.find(query, AvailabilityOverride.class);
    }

    public List<AvailabilityOverride> findByStaffId(String staffId) {
        Query query = scopedQuery(Criteria.where("staffId").is(staffId));
        return template.find(query, AvailabilityOverride.class);
    }
}
