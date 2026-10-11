package au.com.getkempt.request.user;

import jakarta.validation.constraints.Email;

/** {@code PATCH /users/me} body — all fields optional (partial update). */
public record UpdateMeRequest(
        String firstName,
        String lastName,
        @Email String email,
        String avatarUrl) {
}
