package au.com.getkempt.request.availability;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body for {@code POST /availability/{staffId}/copy}. */
public record CopyScheduleRequest(
        @NotNull @Size(min = 1) List<String> targetStaffIds,
        Boolean overwriteOverrides) {
}
