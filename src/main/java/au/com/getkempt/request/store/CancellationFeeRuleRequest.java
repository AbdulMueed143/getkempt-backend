package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.SurchargeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Create/update body for a cancellation fee tier. */
public record CancellationFeeRuleRequest(
        @NotBlank String label,
        @NotNull @PositiveOrZero Integer withinHours,
        @NotNull SurchargeType feeType,
        @NotNull @PositiveOrZero Double feeValue) {
}
