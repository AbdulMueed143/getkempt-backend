package au.com.getkempt.responses.availability;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code WeeklySchedule}. */
public record WeeklyScheduleResponse(
        String staffId,
        String timezone,
        List<DayScheduleResponse> days,
        Instant updatedAt,
        Long version) {
}
