package au.com.getkempt.request.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body for {@code POST /availability/bulk-update} — owner-only, shop-wide hours wizard. */
public record BulkAvailabilityUpdateRequest(@NotNull @Size(min = 1) @Valid List<StaffDaysUpdate> updates) {

    public record StaffDaysUpdate(
            @NotBlank String staffId,
            @NotNull @Size(min = 7, max = 7) @Valid List<DayScheduleRequest> days) {
    }
}
