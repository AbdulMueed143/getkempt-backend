package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleRequest(
        @NotNull Instant newStartAt,
        String newStaffId,
        String reason,
        Boolean notifyClient) {
}
