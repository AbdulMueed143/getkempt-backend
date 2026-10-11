package au.com.getkempt.request.store;

import jakarta.validation.constraints.NotNull;

/** {@code PUT /store/privacy-policy} body. */
public record SetPrivacyPolicyRequest(@NotNull String text) {
}
