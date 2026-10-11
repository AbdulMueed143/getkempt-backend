package au.com.getkempt.repositories.staff;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.StaffInvitation;
import au.com.getkempt.enums.staff.InvitationStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped data access for {@link StaffInvitation}. */
@Repository
public class StaffInvitationRepository extends TenantRepository<StaffInvitation> {

    public StaffInvitationRepository(MongoTemplate template) {
        super(template, StaffInvitation.class, IdPrefix.STAFF_INVITATION);
    }

    public List<StaffInvitation> findAllActive() {
        return template.find(
                scopedQuery(Criteria.where("status").is(InvitationStatus.PENDING)),
                StaffInvitation.class);
    }

    public Optional<StaffInvitation> findPendingByEmail(String email) {
        Query q = scopedQuery(Criteria.where("email").is(email)
                .and("status").is(InvitationStatus.PENDING));
        return Optional.ofNullable(template.findOne(q, StaffInvitation.class));
    }
}
