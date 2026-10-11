package au.com.getkempt.request.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Body for {@code POST /availability/{staffId}/overrides}. */
public record OverrideCreateRequest(
        @NotNull LocalDate date,
        @NotNull Boolean isWorking,
        @Valid List<TimeSlotRequest> slots,
        String note) {
}
