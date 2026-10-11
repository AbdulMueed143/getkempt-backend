package au.com.getkempt.request.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/** Wire shape of the contract's {@code DaySchedule} on write. */
public record DayScheduleRequest(
        @Min(0) @Max(6) int dayOfWeek,
        boolean isWorking,
        @Valid List<TimeSlotRequest> slots) {
}
