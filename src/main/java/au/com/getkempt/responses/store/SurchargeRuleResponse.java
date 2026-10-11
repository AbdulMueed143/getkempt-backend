package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.SurchargeDay;
import au.com.getkempt.enums.store.SurchargeType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Wire shape of the contract's {@code SurchargeRule}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SurchargeRuleResponse(
        String id,
        String name,
        boolean isActive,
        List<SurchargeDay> days,
        String timeStart,
        String timeEnd,
        SurchargeType surchargeType,
        Double surchargeValue) {
}
