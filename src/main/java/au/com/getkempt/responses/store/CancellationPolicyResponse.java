package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.FeeType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Wire shape of the {@code GET /store/cancellation-policy} response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CancellationPolicyResponse(
        String policyText,
        FeeType noShowFeeType,
        Double noShowFeeValue,
        boolean autoCharge,
        List<CancellationFeeRuleResponse> rules) {
}
