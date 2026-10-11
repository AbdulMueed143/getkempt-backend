package au.com.getkempt.request.booking;

import au.com.getkempt.enums.booking.BookingSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Body for {@code POST /bookings} — see {@code docs/05-booking-lifecycle.md}. */
public record BookingCreateRequest(
        @NotBlank String staffId,
        String serviceId,
        String packageId,
        @NotNull Instant startAt,
        String clientId,
        @Valid InlineClientRequest client,
        BookingSource source,
        String notes,
        Boolean sendConfirmation,
        Boolean skipConflictCheck) {
}
