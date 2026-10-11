package au.com.getkempt.entities;

import au.com.getkempt.enums.UserRole;
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
 * A login account. Unlike most aggregates, a {@code User} is looked up cross-tenant
 * by email (at login, before any tenant is resolved), so it is NOT a
 * {@code TenantRepository} entity — it carries its {@code shopId} but is queried by
 * email directly. Password is stored as an argon2id hash.
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String businessName;

    /** Avatar storage key; the URL is derived at read time. */
    private String avatarKey;

    /** Tenant the user belongs to (the shop created at signup). */
    @Indexed
    private String shopId;

    /** Non-null once the email is verified. */
    private Instant emailVerifiedAt;

    private String emailVerificationTokenHash;
    private String passwordResetTokenHash;
    private Instant passwordResetExpiresAt;

    /** Drives Spring Data "is-new" detection (id is pre-assigned) + optimistic locking. */
    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}
