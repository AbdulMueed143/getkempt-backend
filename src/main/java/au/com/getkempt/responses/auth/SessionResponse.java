package au.com.getkempt.responses.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the contract's {@code Session} (listed under {@code /users/me/sessions}). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(
        String id,
        String deviceName,
        String ip,
        String location,
        Instant createdAt,
        Instant lastSeenAt,
        boolean current) {
}
