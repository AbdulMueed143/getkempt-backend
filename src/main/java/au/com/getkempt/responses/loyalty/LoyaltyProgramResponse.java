package au.com.getkempt.responses.loyalty;

import au.com.getkempt.enums.loyalty.ProgramStatus;
import au.com.getkempt.enums.loyalty.RedeemLocation;
import au.com.getkempt.enums.loyalty.RewardType;
import au.com.getkempt.enums.loyalty.TriggerType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;

/** Wire shape of the contract's {@code LoyaltyProgram}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoyaltyProgramResponse(
        String id,
        int sequenceOrder,
        String name,
        String description,
        RewardType rewardType,
        Double rewardValue,
        String rewardLabel,
        RedeemLocation redeemLocation,
        String partnerName,
        String partnerAddress,
        TriggerType triggerType,
        double triggerValue,
        LocalDate programExpiryDate,
        Integer rewardExpiryDays,
        ProgramStatus status,
        int totalEarned,
        int totalRedeemed,
        Instant createdAt,
        Instant updatedAt) {
}
