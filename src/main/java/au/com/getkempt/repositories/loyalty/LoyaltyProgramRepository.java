package au.com.getkempt.repositories.loyalty;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.loyalty.LoyaltyProgram;
import au.com.getkempt.enums.loyalty.ProgramStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped data access for {@link LoyaltyProgram}. */
@Repository
public class LoyaltyProgramRepository extends TenantRepository<LoyaltyProgram> {

    public LoyaltyProgramRepository(MongoTemplate template) {
        super(template, LoyaltyProgram.class, IdPrefix.LOYALTY_PROGRAM);
    }

    public List<LoyaltyProgram> listInSequenceOrder(ProgramStatus status) {
        Criteria criteria = status != null ? Criteria.where("status").is(status) : new Criteria();
        Query query = scopedQuery(criteria).with(Sort.by(Sort.Direction.ASC, "sequenceOrder"));
        return template.find(query, LoyaltyProgram.class);
    }

    /** The lowest-{@code sequenceOrder} ACTIVE program with a sequence strictly after {@code afterSequenceOrder}. */
    public Optional<LoyaltyProgram> nextActiveAfter(int afterSequenceOrder) {
        Query query = scopedQuery(Criteria.where("status").is(ProgramStatus.ACTIVE).and("sequenceOrder").gt(afterSequenceOrder))
                .with(Sort.by(Sort.Direction.ASC, "sequenceOrder")).limit(1);
        return Optional.ofNullable(template.findOne(query, LoyaltyProgram.class));
    }

    public Optional<LoyaltyProgram> firstActive() {
        Query query = scopedQuery(Criteria.where("status").is(ProgramStatus.ACTIVE))
                .with(Sort.by(Sort.Direction.ASC, "sequenceOrder")).limit(1);
        return Optional.ofNullable(template.findOne(query, LoyaltyProgram.class));
    }

    public int maxSequenceOrder() {
        Query query = scopedQuery(null).with(Sort.by(Sort.Direction.DESC, "sequenceOrder")).limit(1);
        LoyaltyProgram top = template.findOne(query, LoyaltyProgram.class);
        return top == null ? 0 : top.getSequenceOrder();
    }
}
