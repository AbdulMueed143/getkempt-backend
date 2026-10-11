package au.com.getkempt.request;

import jakarta.validation.constraints.NotBlank;

/** Shared body for "set image" endpoints (avatar, logo): references a prior upload. */
public record UploadRefRequest(@NotBlank String uploadId) {
}
