package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record ConflictCheckRequest(
        @NotBlank String staffId,
        @NotNull Instant startAt,
        @Positive int durationMinutes,
        String ignoreBookingId) {
}
