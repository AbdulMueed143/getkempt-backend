package au.com.getkempt.entities.upload;

import au.com.getkempt.enums.upload.UploadPurpose;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Bridges {@code POST /uploads/presign} to {@code POST /uploads/{id}/confirm}:
 * the confirm step takes only a path id, so the {@code key} it needs to verify
 * against object storage has to have been remembered server-side since presign
 * time. Self-expires via a TTL index (same idiom as {@code IdempotencyRecord})
 * so an abandoned upload doesn't linger forever; not tenant-scoped through
 * {@code TenantRepository} since {@code uploadId} is itself an unguessable
 * server-minted UUID, same reasoning as idempotency records.
 */
@Document(collection = "pending_uploads")
@Getter
@Setter
@Builder
public class PendingUpload {

    @Id
    private String id;

    private String shopId;
    private UploadPurpose purpose;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String key;
    private boolean confirmed;

    @Indexed(expireAfter = "1h")
    private Instant createdAt;
}
