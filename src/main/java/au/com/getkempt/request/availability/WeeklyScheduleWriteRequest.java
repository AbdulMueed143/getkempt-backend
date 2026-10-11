package au.com.getkempt.request.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body for {@code PUT /availability/{staffId}/weekly} — a full replace. */
public record WeeklyScheduleWriteRequest(
        @NotBlank String timezone,
        @NotNull @Size(min = 7, max = 7) @Valid List<DayScheduleRequest> days) {
}
