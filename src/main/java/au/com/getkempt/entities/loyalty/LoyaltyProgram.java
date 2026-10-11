package au.com.getkempt.entities.loyalty;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.loyalty.ProgramStatus;
import au.com.getkempt.enums.loyalty.RedeemLocation;
import au.com.getkempt.enums.loyalty.RewardType;
import au.com.getkempt.enums.loyalty.TriggerType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * One rung of the shop's loyalty ladder. Clients progress through active
 * programs strictly in {@code sequenceOrder} — see {@code paths/loyalty.yaml}.
 * {@code triggerValue} for {@link TriggerType#SPEND} is in integer cents, same
 * convention as everywhere else money is counted (contract §5).
 */
@Document(collection = "loyalty_programs")
@Getter
@Setter
public class LoyaltyProgram extends BaseDocument {

    private int sequenceOrder;
    private String name;
    private String description;

    private RewardType rewardType;
    private Double rewardValue;
    private String rewardLabel;
    private RedeemLocation redeemLocation;
    private String partnerName;
    private String partnerAddress;

    private TriggerType triggerType;
    private double triggerValue;

    private LocalDate programExpiryDate;
    private Integer rewardExpiryDays;

    private ProgramStatus status;
    private int totalEarned;
    private int totalRedeemed;
}
