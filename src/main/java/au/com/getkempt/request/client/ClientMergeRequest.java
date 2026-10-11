package au.com.getkempt.request.client;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /clients/{id}/merge} — {@code sourceClientId} is absorbed into the path id. */
public record ClientMergeRequest(@NotBlank String sourceClientId) {
}
