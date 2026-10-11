package au.com.getkempt.request.availability;

import au.com.getkempt.enums.availability.SlotType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Wire shape of the contract's {@code TimeSlot} on write. {@code id} is optional — assigned server-side when absent. */
public record TimeSlotRequest(
        String id,
        @NotNull @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") String startTime,
        @NotNull @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") String endTime,
        @NotNull SlotType type,
        String label) {
}
