package au.com.getkempt.responses.store;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the {@code GET /store/privacy-policy} response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrivacyPolicyResponse(String text, Instant updatedAt) {
}
