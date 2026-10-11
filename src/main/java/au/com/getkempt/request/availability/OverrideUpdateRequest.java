package au.com.getkempt.request.availability;

import jakarta.validation.Valid;

import java.util.List;

/** PATCH body for {@code /availability/{staffId}/overrides/{overrideId}} — all fields optional. */
public record OverrideUpdateRequest(
        Boolean isWorking,
        @Valid List<TimeSlotRequest> slots,
        String note) {
}
