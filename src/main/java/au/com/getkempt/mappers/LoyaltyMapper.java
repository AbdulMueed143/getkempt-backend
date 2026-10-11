package au.com.getkempt.mappers;

import au.com.getkempt.entities.loyalty.LoyaltyProgram;
import au.com.getkempt.entities.loyalty.LoyaltyReward;
import au.com.getkempt.responses.loyalty.LoyaltyProgramResponse;
import au.com.getkempt.responses.loyalty.LoyaltyRewardResponse;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyMapper {

    public LoyaltyProgramResponse toResponse(LoyaltyProgram program) {
        return new LoyaltyProgramResponse(
                program.getId(),
                program.getSequenceOrder(),
                program.getName(),
                program.getDescription(),
                program.getRewardType(),
                program.getRewardValue(),
                program.getRewardLabel(),
                program.getRedeemLocation(),
                program.getPartnerName(),
                program.getPartnerAddress(),
                program.getTriggerType(),
                program.getTriggerValue(),
                program.getProgramExpiryDate(),
                program.getRewardExpiryDays(),
                program.getStatus(),
                program.getTotalEarned(),
                program.getTotalRedeemed(),
                program.getCreatedAt(),
                program.getUpdatedAt());
    }

    public LoyaltyRewardResponse toResponse(LoyaltyReward reward) {
        return new LoyaltyRewardResponse(
                reward.getId(),
                reward.getClientId(),
                reward.getProgramId(),
                reward.getStatus().value(),
                reward.getEarnedAt(),
                reward.getRedeemedAt(),
                reward.getExpiresAt(),
                reward.getRedeemedAtBookingId());
    }
}
