package au.com.getkempt.request.upload;

import au.com.getkempt.enums.upload.UploadPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record UploadPresignRequest(
        @NotNull UploadPurpose purpose,
        @NotBlank String filename,
        @NotBlank @Pattern(regexp = "^image/(jpeg|png|webp|heic)$") String contentType,
        @Positive @Max(10_485_760) long sizeBytes) {
}
