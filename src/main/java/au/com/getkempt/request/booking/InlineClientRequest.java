package au.com.getkempt.request.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code BookingCreateRequest.client} — create a client inline for walk-ins. */
public record InlineClientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        @NotBlank String phone) {
}
