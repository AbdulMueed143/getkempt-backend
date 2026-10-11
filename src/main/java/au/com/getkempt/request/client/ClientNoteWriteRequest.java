package au.com.getkempt.request.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create body for {@code POST /clients/{id}/notes}. */
public record ClientNoteWriteRequest(
        @NotBlank @Size(max = 4000) String body,
        Boolean pinned) {
}
