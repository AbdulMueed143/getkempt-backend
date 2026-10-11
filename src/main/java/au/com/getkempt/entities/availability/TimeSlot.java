package au.com.getkempt.entities.availability;

import au.com.getkempt.enums.availability.SlotType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single work-or-break window within a day, embedded in {@link DaySchedule} (the
 * weekly recurring schedule) or {@link AvailabilityOverride} (a date-specific one).
 *
 * <p>{@code startTime}/{@code endTime} are {@code HH:MM} shop-local wall-clock time
 * (per {@code docs/01-conventions.md §4}) — never a UTC instant, since the same
 * schedule must render correctly regardless of DST transitions.
 *
 * <p>Recurring breaks (see {@code /availability/{staffId}/breaks}) are ordinary
 * {@code type=break} slots that share the SAME {@code id} across every day they
 * apply to, so one logical break can be edited/removed everywhere in one call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    private String id;
    private String startTime;
    private String endTime;
    private SlotType type;
    private String label;
}
