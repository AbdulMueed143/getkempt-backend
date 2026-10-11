package au.com.getkempt.entities.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Records that a shop dismissed a given (computed, not persisted) dashboard
 * alert. Dashboard alerts themselves are computed fresh on every request from
 * live state (see {@code DashboardService}) rather than stored — there's
 * nothing to "own" the alert row otherwise — so {@code alertKey} is a stable,
 * deterministic id (e.g. {@code "pending_review:2026-07-03"}) the alert
 * generator and the dismiss endpoint both derive the same way.
 */
@Document(collection = "dismissed_dashboard_alerts")
@CompoundIndex(name = "uniq_shop_alert", def = "{'shopId': 1, 'alertKey': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DismissedAlert {

    @Id
    private String id;

    private String shopId;
    private String alertKey;
    private Instant dismissedAt;
}
