package au.com.getkempt.responses.loyalty;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoyaltyStatsResponse(
        int totalEarned,
        int totalRedeemed,
        double redemptionRatePercent,
        TopProgram topProgram) {

    public record TopProgram(String programId, String name, int count) {
    }
}
