package au.com.getkempt.responses.auth;

import au.com.getkempt.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the contract's {@code User}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        String id,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName,
        UserRole role,
        String businessName,
        String avatarUrl,
        String shopId,
        Instant createdAt) {
}
