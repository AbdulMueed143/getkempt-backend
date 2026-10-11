package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingNotesRequest(@NotNull @Size(max = 2000) String notes) {
}
