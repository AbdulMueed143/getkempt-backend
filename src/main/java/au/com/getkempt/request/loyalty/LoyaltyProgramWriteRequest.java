package au.com.getkempt.request.loyalty;

import au.com.getkempt.enums.loyalty.ProgramStatus;
import au.com.getkempt.enums.loyalty.RedeemLocation;
import au.com.getkempt.enums.loyalty.RewardType;
import au.com.getkempt.enums.loyalty.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Create/update body for a loyalty program — POST validates, PATCH applies non-null fields only. */
public record LoyaltyProgramWriteRequest(
        @NotBlank String name,
        String description,
        @NotNull RewardType rewardType,
        Double rewardValue,
        @NotBlank String rewardLabel,
        @NotNull RedeemLocation redeemLocation,
        String partnerName,
        String partnerAddress,
        @NotNull TriggerType triggerType,
        @Positive Double triggerValue,
        LocalDate programExpiryDate,
        Integer rewardExpiryDays,
        ProgramStatus status) {
}
