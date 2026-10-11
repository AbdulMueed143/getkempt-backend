package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.FeeType;

import java.util.List;

/** {@code PUT /store/cancellation-policy} body — replaces policy + all fee tiers. */
public record SetCancellationPolicyRequest(
        String policyText,
        FeeType noShowFeeType,
        Double noShowFeeValue,
        Boolean autoCharge,
        List<CancellationFeeRuleRequest> rules) {
}
