package au.com.getkempt.request.client;

import jakarta.validation.constraints.Size;

/** PATCH body for {@code /clients/{id}/notes/{noteId}} — both fields optional. */
public record ClientNoteUpdateRequest(
        @Size(max = 4000) String body,
        Boolean pinned) {
}
