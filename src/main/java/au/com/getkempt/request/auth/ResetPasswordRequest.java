package au.com.getkempt.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code POST /auth/reset-password} body. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "must be at least 8 chars and include an uppercase letter and a digit")
        String password) {
}
