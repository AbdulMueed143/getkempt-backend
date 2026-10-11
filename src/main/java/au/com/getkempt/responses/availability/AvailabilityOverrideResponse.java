package au.com.getkempt.responses.availability;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Wire shape of the contract's {@code AvailabilityOverride}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityOverrideResponse(
        String id,
        String staffId,
        LocalDate date,
        boolean isWorking,
        List<TimeSlotResponse> slots,
        String note,
        Instant createdAt) {
}
