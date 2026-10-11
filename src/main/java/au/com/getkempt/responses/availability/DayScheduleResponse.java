package au.com.getkempt.responses.availability;

import java.util.List;

/** Wire shape of the contract's {@code DaySchedule}. */
public record DayScheduleResponse(int dayOfWeek, boolean isWorking, List<TimeSlotResponse> slots) {
}
