package au.com.getkempt.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/forgot-password} body. */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}
