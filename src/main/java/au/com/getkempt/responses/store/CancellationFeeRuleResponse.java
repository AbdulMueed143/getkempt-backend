package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.SurchargeType;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of the contract's {@code CancellationFeeRule}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CancellationFeeRuleResponse(
        String id,
        String label,
        int withinHours,
        SurchargeType feeType,
        Double feeValue) {
}
