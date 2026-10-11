package au.com.getkempt.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A sign-in session — the server-side anchor for a refresh-token chain and the unit
 * of revocation ({@code jti}). The opaque refresh token is stored only as a SHA-256
 * hash; rotation overwrites {@code refreshTokenHash} so a replayed (already-rotated)
 * token no longer matches. Access tokens carry this session's id as their {@code jti}.
 */
@Document(collection = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    /** Session id == JWT {@code jti}. */
    @Id
    private String id;

    @Indexed
    private String userId;

    private String shopId;

    @Indexed
    private String refreshTokenHash;

    /**
     * The immediately-prior refresh hash. If a client presents this (already-rotated)
     * token, it is treated as a replay/compromise and the whole session is revoked
     * ({@code docs/02-authentication.md}).
     */
    @Indexed
    private String previousRefreshTokenHash;

    private String deviceName;
    private String ip;
    private String location;
    private String userAgent;

    /** Drives Spring Data "is-new" detection (id is pre-assigned). */
    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    private Instant lastSeenAt;
    private Instant expiresAt;
    private Instant revokedAt;

    public boolean isActive(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }
}
