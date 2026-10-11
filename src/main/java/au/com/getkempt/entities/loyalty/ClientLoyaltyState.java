package au.com.getkempt.entities.loyalty;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-client progress ledger — one document per client, {@code id == clientId}
 * (same deterministic-key idiom as {@code WeeklySchedule}/{@code StoreSettings}).
 *
 * <p>Not part of the public contract directly ({@code ClientLoyaltyProgress} is
 * the wire shape, derived from this); exists because "progress toward the
 * current program" is inherently stateful across a sequence of programs with
 * potentially different trigger types (visits vs. spend), so it can't be
 * recomputed purely from {@code Client.totalBookings}/{@code lifetimeSpendCents}
 * without remembering where the current program's counting started.
 */
@Document(collection = "client_loyalty_state")
@Getter
@Setter
public class ClientLoyaltyState extends BaseDocument {

    private String clientId;
    private String currentProgramId;

    /** The client's trigger-unit counter value (visits or spend cents) when {@code currentProgramId} started. */
    private double baselineValue;

    private List<HistoryEntry> history = new ArrayList<>();

    @Getter
    @Setter
    public static class HistoryEntry {
        private String programId;
        private Instant earnedAt;
        private Instant redeemedAt;
    }
}
