package au.com.getkempt.entities.availability;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A staff member's recurring weekly schedule — exactly one per staff member, same
 * "id IS the natural key" idiom as {@code StoreProfile}/{@code StoreSettings}
 * (here {@code id == staffId}, rather than minting a fresh UUID), so callers never
 * have to look up a schedule id before reading/writing it. {@code shopId} is still
 * stamped (via {@code TenantRepository}) so tenant scoping is defense-in-depth, not
 * solely reliant on the staff id being unguessable.
 *
 * <p>Carries {@code @Version} for optimistic concurrency (contract §11).
 */
@Document(collection = "weekly_schedules")
@Getter
@Setter
public class WeeklySchedule extends BaseDocument {

    private String staffId;
    private String timezone;

    private List<DaySchedule> days = defaultDays();

    /** All 7 days off — the starting point for a newly-created staff member. */
    public static List<DaySchedule> defaultDays() {
        List<DaySchedule> days = new ArrayList<>(7);
        for (int day = 0; day < 7; day++) {
            days.add(DaySchedule.builder().dayOfWeek(day).isWorking(false).slots(new ArrayList<>()).build());
        }
        return days;
    }
}
