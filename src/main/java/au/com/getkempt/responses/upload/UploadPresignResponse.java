package au.com.getkempt.responses.upload;

import java.time.Instant;
import java.util.Map;

public record UploadPresignResponse(
        String uploadId,
        String uploadUrl,
        Map<String, String> fields,
        String key,
        Instant expiresAt) {
}
