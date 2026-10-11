package au.com.getkempt.integrations.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/** Local storage stub — returns fake presigned URLs and a local-style public URL. */
@Component
@Slf4j
public class StubFileStorage implements FileStorage {

    @Override
    public PresignedUpload presign(String purpose, String filename, String contentType, long sizeBytes) {
        String uploadId = "upl_stub_" + UUID.randomUUID().toString().replace("-", "");
        String key = "%s/%s/%s".formatted(purpose, uploadId, filename);
        log.info("[stub-storage] presign purpose={} key={} contentType={} bytes={}",
                purpose, key, contentType, sizeBytes);
        return new PresignedUpload(
                uploadId,
                "https://uploads.local.getkempt.test/" + key,
                Map.of("key", key, "Content-Type", contentType),
                key,
                Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    @Override
    public String publicUrl(String key) {
        return "https://cdn.local.getkempt.test/" + key;
    }
}
