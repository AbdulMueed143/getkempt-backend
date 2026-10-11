package au.com.getkempt.entities.store;

import au.com.getkempt.enums.store.SurchargeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tier in the cancellation-fee policy, embedded in {@link StoreSettings}. The
 * tightest {@code withinHours} that still covers the time-until-appointment wins
 * ({@code docs/05-booking-lifecycle.md}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationFeeRule {

    private String id;
    private String label;
    private int withinHours;
    private SurchargeType feeType;
    private Double feeValue;
}
