package au.com.getkempt.services.authentication;

import au.com.getkempt.common.error.exceptions.UnauthorizedException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.security.RolePermissions;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.common.security.Tokens;
import au.com.getkempt.configurations.security.filters.JwtTokenProvider;
import au.com.getkempt.configurations.security.properties.JwtProperties;
import au.com.getkempt.entities.Session;
import au.com.getkempt.entities.User;
import au.com.getkempt.enums.CallerType;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.repositories.session.SessionRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.repositories.user.UserRepository;
import au.com.getkempt.responses.auth.AuthTokensResponse;
import is.tagomor.woothee.Classifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the session lifecycle: minting the access JWT + opaque refresh token, rotating
 * refresh tokens (single-use, with replay detection), and revocation. The {@code jti}
 * of every access token IS the {@link Session#getId()}, which is how revocation works.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /** Create a brand-new session for a freshly-authenticated user. */
    public AuthTokensResponse issue(User user, HttpServletRequest request) {
        Instant now = Instant.now();
        String sessionId = IdGenerator.generate(IdPrefix.SESSION);
        String rawRefresh = Tokens.randomToken();

        Session session = Session.builder()
                .id(sessionId)
                .userId(user.getId())
                .shopId(user.getShopId())
                .refreshTokenHash(Tokens.sha256(rawRefresh))
                .deviceName(resolveDeviceName(request))
                .ip(resolveIp(request))
                .userAgent(request == null ? null : request.getHeader("User-Agent"))
                .lastSeenAt(now)
                .expiresAt(now.plus(jwtProperties.getRefreshTokenTTLInDays(), ChronoUnit.DAYS))
                .build();
        sessionRepository.save(session);

        return buildTokens(user, sessionId, rawRefresh);
    }

    /**
     * Rotate a refresh token: validate, single-use it, and issue a fresh pair. A replay
     * of an already-rotated token revokes the whole session.
     */
    public AuthTokensResponse rotate(String rawRefreshToken, HttpServletRequest request) {
        String presentedHash = Tokens.sha256(rawRefreshToken);

        Session session = sessionRepository.findByRefreshTokenHash(presentedHash).orElse(null);
        if (session == null) {
            // Possibly a replayed, already-rotated token → revoke the chain it belonged to.
            sessionRepository.findByPreviousRefreshTokenHash(presentedHash).ifPresent(compromised -> {
                log.warn("Refresh-token replay detected; revoking session {}", compromised.getId());
                compromised.setRevokedAt(Instant.now());
                sessionRepository.save(compromised);
            });
            throw new UnauthorizedException("Refresh token is invalid or has already been used");
        }

        Instant now = Instant.now();
        if (!session.isActive(now)) {
            throw new UnauthorizedException("Session is expired or revoked");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        String rawRefresh = Tokens.randomToken();
        session.setPreviousRefreshTokenHash(session.getRefreshTokenHash());
        session.setRefreshTokenHash(Tokens.sha256(rawRefresh));
        session.setLastSeenAt(now);
        sessionRepository.save(session);

        return buildTokens(user, session.getId(), rawRefresh);
    }

    public void revoke(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setRevokedAt(Instant.now());
            sessionRepository.save(session);
        });
    }

    public void revokeOtherSessions(String userId, String currentSessionId) {
        Instant now = Instant.now();
        List<Session> others = sessionRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(s -> !s.getId().equals(currentSessionId))
                .toList();
        others.forEach(s -> s.setRevokedAt(now));
        sessionRepository.saveAll(others);
    }

    public List<Session> activeSessions(String userId) {
        Instant now = Instant.now();
        return sessionRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(s -> s.isActive(now))
                .toList();
    }

    /** Access-token revocation check used by the JWT filter ({@code jti == sessionId}). */
    public boolean isSessionActive(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        return sessionRepository.findById(sessionId)
                .map(s -> s.isActive(Instant.now()))
                .orElse(false);
    }

    private AuthTokensResponse buildTokens(User user, String sessionId, String rawRefresh) {
        StaffMember staff = staffRepository.findByShopIdAndUserId(user.getShopId(), user.getId()).orElse(null);
        StaffRole role = staff != null ? staff.getRole() : StaffRole.OWNER;

        Map<String, Object> claims = new HashMap<>();
        claims.put("shop_id", user.getShopId());
        if (staff != null) {
            claims.put("staff_id", staff.getId());
        }
        claims.put("role", role.wire());
        claims.put("permissions", List.copyOf(RolePermissions.forRole(role)));
        claims.put("jti", sessionId);

        String accessToken = jwtTokenProvider.generateToken(user.getId(), CallerType.STAFF, claims);
        long expiresInSeconds = jwtProperties.getStaffTokenTTL() * 60L;
        return AuthTokensResponse.bearer(accessToken, rawRefresh, expiresInSeconds);
    }

    private String resolveDeviceName(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return null;
        }
        Map<String, String> parsed = Classifier.parse(ua);
        String name = clean(parsed.get("name"));
        String os = clean(parsed.get("os"));
        if (name == null) {
            return os; // may also be null
        }
        return os == null ? name : name + " on " + os;
    }

    /** woothee returns the literal {@code "UNKNOWN"} when it can't classify; treat as absent. */
    private String clean(String value) {
        if (value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
