package au.com.getkempt.repositories.loyalty;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.loyalty.LoyaltyReward;
import au.com.getkempt.enums.loyalty.RewardStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped data access for {@link LoyaltyReward}. */
@Repository
public class LoyaltyRewardRepository extends TenantRepository<LoyaltyReward> {

    public LoyaltyRewardRepository(MongoTemplate template) {
        super(template, LoyaltyReward.class, IdPrefix.LOYALTY_REWARD);
    }

    public List<LoyaltyReward> findByClientId(String clientId) {
        return template.find(scopedQuery(Criteria.where("clientId").is(clientId)), LoyaltyReward.class);
    }

    public boolean hasPendingForProgram(String programId) {
        return template.exists(scopedQuery(Criteria.where("programId").is(programId).and("status").is(RewardStatus.PENDING)),
                LoyaltyReward.class);
    }
}
