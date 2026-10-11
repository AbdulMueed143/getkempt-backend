package au.com.getkempt.common.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted image reference. Only the {@code key} (storage path) is canonical; the
 * public/CDN URL is derived at read time via {@code FileStorage.publicUrl(key)},
 * never stored, so rotating the CDN never requires a data migration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImageRef {

    private String key;
    private Integer width;
    private Integer height;
}
