package au.com.getkempt.integrations.storage;

import java.time.Instant;
import java.util.Map;

/**
 * Port for object storage + presigned uploads (AWS S3 / Cloudflare R2 in production).
 * Backs the {@code /uploads/*} endpoints for direct-to-storage image uploads.
 */
public interface FileStorage {

    record PresignedUpload(String uploadId, String url, Map<String, String> fields, String key, Instant expiresAt) {
    }

    PresignedUpload presign(String purpose, String filename, String contentType, long sizeBytes);

    /** Resolve the publicly servable (CDN/signed) URL for a stored key. */
    String publicUrl(String key);
}
