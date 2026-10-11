package au.com.getkempt.common.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

/**
 * Base class for every tenant-scoped MongoDB document.
 *
 * <p>Provides the four invariants every persisted aggregate must carry:
 * <ul>
 *   <li>{@code shopId} — the tenant. MongoDB has no Row-Level-Security, so tenancy
 *       is enforced in the data layer ({@code TenantRepository}); this field is the
 *       hook every query filters on.</li>
 *   <li>{@code version} — optimistic locking ({@code If-Match} / 412), per
 *       {@code docs/01-conventions.md §11}.</li>
 *   <li>{@code createdAt} / {@code updatedAt} — audit timestamps (Mongo auditing).</li>
 *   <li>{@code deletedAt} — soft-delete marker, per §12. Non-null rows are excluded
 *       from list endpoints by default.</li>
 * </ul>
 */
@Getter
@Setter
public abstract class BaseDocument {

    @Id
    private String id;

    @Indexed
    private String shopId;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
