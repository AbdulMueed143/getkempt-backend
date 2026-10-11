package au.com.getkempt.entities.notification;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-channel toggles for one staff user; {@code id == userId} (same
 * deterministic-key idiom as {@code WeeklySchedule}).
 */
@Document(collection = "notification_preferences")
@Getter
@Setter
public class NotificationPreferences extends BaseDocument {

    private Map<String, Boolean> inApp = new HashMap<>();
    private Map<String, Boolean> email = new HashMap<>();
    private Map<String, Boolean> push = new HashMap<>();
}
