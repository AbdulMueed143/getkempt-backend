package au.com.getkempt.entities.availability;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A date-specific exception to a staff member's {@link WeeklySchedule} — sick days,
 * half-days, one-off extended hours. Takes precedence over the recurring schedule
 * on its date ({@code docs/05-booking-lifecycle.md} slot-validation step).
 */
@Document(collection = "availability_overrides")
@CompoundIndex(name = "uniq_staff_date", def = "{'shopId': 1, 'staffId': 1, 'date': 1}", unique = true,
        partialFilter = "{'deletedAt': null}")
@Getter
@Setter
public class AvailabilityOverride extends BaseDocument {

    private String staffId;
    private LocalDate date;
    private boolean isWorking;
    private List<TimeSlot> slots = new ArrayList<>();
    private String note;
}
