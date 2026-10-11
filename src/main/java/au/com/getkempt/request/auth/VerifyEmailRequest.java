package au.com.getkempt.request.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/verify-email} body. */
public record VerifyEmailRequest(@NotBlank String token) {
}
