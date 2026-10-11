package au.com.getkempt.repositories.notification;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.notification.PushDevice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped data access for {@link PushDevice}. */
@Repository
public class PushDeviceRepository extends TenantRepository<PushDevice> {

    public PushDeviceRepository(MongoTemplate template) {
        super(template, PushDevice.class, IdPrefix.PUSH_DEVICE);
    }

    public List<PushDevice> findByUserId(String userId) {
        return template.find(scopedQuery(Criteria.where("userId").is(userId)), PushDevice.class);
    }

    public Optional<PushDevice> findByUserIdAndToken(String userId, String token) {
        return Optional.ofNullable(template.findOne(
                scopedQuery(Criteria.where("userId").is(userId).and("token").is(token)), PushDevice.class));
    }
}
