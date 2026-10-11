package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookingBulkRequest(
        @NotEmpty List<String> bookingIds,
        @NotNull String action,
        String reason,
        Boolean notifyClients) {
}
