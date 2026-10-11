package au.com.getkempt.responses.client;

import au.com.getkempt.responses.loyalty.LoyaltyRewardResponse;

import java.util.List;

/** Wire shape for {@code GET /clients/{id}/loyalty}. */
public record ClientLoyaltyResponse(
        ClientLoyaltyProgressResponse progress,
        List<LoyaltyRewardResponse> rewards) {
}
