package au.com.getkempt.entities.store;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.enums.store.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * The tenant root: exactly one per shop. The document {@code id} IS the {@code shopId}
 * sealed into every JWT, so tenant isolation for this aggregate is the identity itself —
 * the service always loads it by the caller's shop. Carries {@code @Version} for
 * optimistic concurrency ({@code If-Match} → 412).
 */
@Document(collection = "store_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    private String shopName;
    private BusinessType businessType;
    private String description;
    private StoredImageRef logo;
    private String phone;
    private String email;
    private String website;
    private StoreAddress address;
    private String timezone;
    private String instagram;
    private String facebook;
    private String tiktok;

    /** Google Places enrichment for the shop's linked listing — see {@link StoreGooglePlace} javadoc. */
    private StoreGooglePlace googlePlace;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;
}
