package au.com.getkempt.responses.loyalty;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Wire shape of the contract's {@code LoyaltyReward}. Defined ahead of the Loyalty
 * domain itself (not yet implemented) so {@code GET /clients/{id}/loyalty} can
 * return a contract-correct, always-empty {@code rewards[]} today; the Loyalty
 * service will populate it once built, with no wire-format change.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoyaltyRewardResponse(
        String id,
        String clientId,
        String programId,
        String status,
        Instant earnedAt,
        Instant redeemedAt,
        Instant expiresAt,
        String redeemedAtBookingId) {
}
