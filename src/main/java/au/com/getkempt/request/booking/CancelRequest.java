package au.com.getkempt.request.booking;

import au.com.getkempt.enums.booking.CancellationReason;
import jakarta.validation.constraints.NotNull;

public record CancelRequest(
        @NotNull CancellationReason reason,
        String note,
        Boolean chargeFee,
        Boolean notifyClient) {
}
