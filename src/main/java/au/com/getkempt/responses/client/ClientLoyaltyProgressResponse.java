package au.com.getkempt.responses.client;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code ClientLoyaltyProgress}. */
public record ClientLoyaltyProgressResponse(
        String clientId,
        String currentProgramId,
        double progressValue,
        double targetValue,
        List<HistoryEntry> history) {

    public record HistoryEntry(String programId, Instant earnedAt, Instant redeemedAt) {
    }
}
