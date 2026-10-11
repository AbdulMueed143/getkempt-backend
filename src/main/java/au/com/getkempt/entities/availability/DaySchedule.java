package au.com.getkempt.entities.availability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * One day of a {@link WeeklySchedule}. {@code dayOfWeek} follows the contract's
 * JS-style convention: 0 = Sunday, 6 = Saturday.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaySchedule {

    private int dayOfWeek;
    private boolean isWorking;

    @Builder.Default
    private List<TimeSlot> slots = new ArrayList<>();
}
