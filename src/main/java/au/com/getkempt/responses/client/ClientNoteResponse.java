package au.com.getkempt.responses.client;

import java.time.Instant;

/** Wire shape of the contract's {@code ClientNote}. */
public record ClientNoteResponse(
        String id,
        String body,
        boolean pinned,
        Instant createdAt,
        String createdByStaffId) {
}
