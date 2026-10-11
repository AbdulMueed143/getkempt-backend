package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.SurchargeDay;
import au.com.getkempt.enums.store.SurchargeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Create/update body for a surcharge rule. */
public record SurchargeRuleRequest(
        @NotBlank String name,
        Boolean isActive,
        @NotNull List<SurchargeDay> days,
        String timeStart,
        String timeEnd,
        @NotNull SurchargeType surchargeType,
        @NotNull Double surchargeValue) {
}
