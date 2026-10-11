package au.com.getkempt.entities.loyalty;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.loyalty.RewardStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A reward earned by a client under a {@link LoyaltyProgram} — pending until redeemed. */
@Document(collection = "loyalty_rewards")
@Getter
@Setter
public class LoyaltyReward extends BaseDocument {

    @Indexed
    private String clientId;
    private String programId;
    private RewardStatus status;
    private Instant earnedAt;
    private Instant redeemedAt;
    private Instant expiresAt;
    private String redeemedAtBookingId;
    private String revokeReason;
}
