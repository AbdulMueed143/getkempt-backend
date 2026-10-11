package au.com.getkempt.repositories.outbox;

import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.entities.outbox.OutboxMessage;
import au.com.getkempt.enums.outbox.OutboxStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain (non-tenant-scoped) data access for {@link OutboxMessage} — see the
 * entity javadoc for why this deliberately does not extend {@code TenantRepository}.
 */
@Repository
public class OutboxMessageRepository {

    private final MongoTemplate template;

    public OutboxMessageRepository(MongoTemplate template) {
        this.template = template;
    }

    public OutboxMessage insert(OutboxMessage message) {
        if (message.getId() == null) {
            message.setId(IdGenerator.generate(IdPrefix.OUTBOX_MESSAGE));
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }
        if (message.getNextAttemptAt() == null) {
            message.setNextAttemptAt(message.getCreatedAt());
        }
        message.setStatus(OutboxStatus.PENDING);
        message.setAttempts(0);
        return template.insert(message);
    }

    /**
     * Finds up to {@code limit} due rows and atomically claims each one
     * ({@code PENDING -> PROCESSING}) one at a time so a concurrent poller (or a
     * second app instance) can never double-claim the same row — see the
     * {@code OutboxStatus.PROCESSING} javadoc.
     */
    public List<OutboxMessage> claimBatch(int limit) {
        Query dueQuery = new Query(Criteria.where("status").is(OutboxStatus.PENDING)
                .and("nextAttemptAt").lte(Instant.now()))
                .limit(limit)
                .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt"));
        List<OutboxMessage> due = template.find(dueQuery, OutboxMessage.class);

        List<OutboxMessage> claimed = new ArrayList<>(due.size());
        for (OutboxMessage candidate : due) {
            Query claimQuery = new Query(Criteria.where("_id").is(candidate.getId()).and("status").is(OutboxStatus.PENDING));
            Update claimUpdate = Update.update("status", OutboxStatus.PROCESSING).set("claimedAt", Instant.now());
            var result = template.updateFirst(claimQuery, claimUpdate, OutboxMessage.class);
            if (result.getModifiedCount() == 1) {
                candidate.setStatus(OutboxStatus.PROCESSING);
                claimed.add(candidate);
            }
            // modifiedCount == 0 means another dispatcher tick/instance already claimed it — skip silently.
        }
        return claimed;
    }

    public void markSent(String id, String providerMessageId) {
        Update update = Update.update("status", OutboxStatus.SENT)
                .set("providerMessageId", providerMessageId)
                .set("sentAt", Instant.now())
                .set("lastError", null);
        template.updateFirst(new Query(Criteria.where("_id").is(id)), update, OutboxMessage.class);
    }

    public void markFailed(String id, int attempts, String error, Instant nextAttemptAt, boolean dead) {
        Update update = Update.update("attempts", attempts)
                .set("status", dead ? OutboxStatus.DEAD : OutboxStatus.FAILED)
                .set("lastError", error);
        if (!dead) {
            // Re-queue for another pass: FAILED is a transient waypoint, the row
            // becomes PENDING again once nextAttemptAt is in the past.
            update.set("status", OutboxStatus.PENDING).set("nextAttemptAt", nextAttemptAt);
        }
        template.updateFirst(new Query(Criteria.where("_id").is(id)), update, OutboxMessage.class);
    }
}
