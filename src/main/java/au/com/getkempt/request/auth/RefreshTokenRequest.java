package au.com.getkempt.request.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/refresh} body. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
