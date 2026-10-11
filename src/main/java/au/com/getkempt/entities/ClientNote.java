package au.com.getkempt.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A free-text note attached to a {@link Client}, embedded on the client document
 * (same pattern as {@code Holiday}/{@code SurchargeRule} on {@code StoreSettings}):
 * notes are always read/written together with their client, so one document keeps
 * the write atomic and avoids an extra round-trip for what is a lightweight,
 * low-cardinality sub-resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientNote {

    private String id;
    private String body;
    private boolean pinned;
    private Instant createdAt;
    private String createdByStaffId;
}
