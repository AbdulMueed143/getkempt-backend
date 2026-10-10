package au.com.getkempt.configurations.security.filters;

import au.com.getkempt.configurations.security.properties.JwtProperties;
import au.com.getkempt.enums.CallerType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;


    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate a JWT with caller-type-specific TTL and claims.
     */
    public String generateToken(String subject, CallerType audience, Map<String, Object> extraClaims) {
        final long ttlMinutes = resolveTtl(audience);
        final Instant now = Instant.now();
        final Instant expiry = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .audience().add(audience.name()).and();

        extraClaims.forEach(builder::claim);

        return builder.signWith(signingKey).compact();
    }

    /**
     * Generate a long-lived refresh token with minimal claims.
     */
    public String generateRefreshToken(String subject, CallerType audience) {
        final Instant now = Instant.now();
        final Instant expiry = now.plus(jwtProperties.getRefreshTokenTTLInDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .audience().add(audience.name()).and()
                .claim("type", "refresh")
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and validate token. Returns claims if valid, throws on any failure.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Parse, validate, AND enforce the audience matches the expected caller type.
     * This is the core cross-domain protection — a CLIENT token can never
     * satisfy a STAFF filter chain and vice versa.
     */
    public Claims parseAndValidateForAudience(String token, CallerType expectedAudience) {
        final Claims claims = parseAndValidate(token);
        final java.util.Set<String> tokenAudiences = claims.getAudience();

        if (tokenAudiences == null || !tokenAudiences.contains(expectedAudience.name())) {
            log.warn("Audience mismatch: expected={}, got={}, sub={}",
                    expectedAudience, tokenAudiences, claims.getSubject());
            throw new JwtException("Token audience mismatch");
        }

        return claims;
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    private long resolveTtl(CallerType callerType) {
        return switch (callerType) {
            case STAFF -> jwtProperties.getStaffTokenTTL();
            case CLIENT -> jwtProperties.getClientTokenTTL();
        };
    }
}