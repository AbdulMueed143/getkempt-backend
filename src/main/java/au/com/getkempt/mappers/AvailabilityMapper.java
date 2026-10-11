package au.com.getkempt.mappers;

import au.com.getkempt.entities.availability.AvailabilityOverride;
import au.com.getkempt.entities.availability.DaySchedule;
import au.com.getkempt.entities.availability.TimeSlot;
import au.com.getkempt.entities.availability.WeeklySchedule;
import au.com.getkempt.responses.availability.AvailabilityOverrideResponse;
import au.com.getkempt.responses.availability.DayScheduleResponse;
import au.com.getkempt.responses.availability.TimeSlotResponse;
import au.com.getkempt.responses.availability.WeeklyScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvailabilityMapper {

    public TimeSlotResponse toResponse(TimeSlot slot) {
        return new TimeSlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getType(), slot.getLabel());
    }

    public DayScheduleResponse toResponse(DaySchedule day) {
        return new DayScheduleResponse(day.getDayOfWeek(), day.isWorking(), toSlotResponses(day.getSlots()));
    }

    public WeeklyScheduleResponse toResponse(WeeklySchedule schedule) {
        return new WeeklyScheduleResponse(
                schedule.getStaffId(),
                schedule.getTimezone(),
                schedule.getDays().stream().map(this::toResponse).toList(),
                schedule.getUpdatedAt(),
                schedule.getVersion());
    }

    public AvailabilityOverrideResponse toResponse(AvailabilityOverride override) {
        return new AvailabilityOverrideResponse(
                override.getId(),
                override.getStaffId(),
                override.getDate(),
                override.isWorking(),
                toSlotResponses(override.getSlots()),
                override.getNote(),
                override.getCreatedAt());
    }

    private List<TimeSlotResponse> toSlotResponses(List<TimeSlot> slots) {
        return slots == null ? List.of() : slots.stream().map(this::toResponse).toList();
    }
}
