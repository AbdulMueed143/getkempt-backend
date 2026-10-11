package au.com.getkempt.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code POST /users/me/password} body. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "must be at least 8 chars and include an uppercase letter and a digit")
        String newPassword) {
}
