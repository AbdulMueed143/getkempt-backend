package au.com.getkempt.entities.booking;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.store.SurchargeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A surcharge snapshotted onto a {@link Booking} at creation/reschedule time. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedSurcharge {

    private String label;
    private SurchargeType type;
    private Double value;
    private Money amount;
}
