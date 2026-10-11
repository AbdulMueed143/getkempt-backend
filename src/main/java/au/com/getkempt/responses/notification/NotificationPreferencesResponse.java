package au.com.getkempt.responses.notification;

import java.util.Map;

public record NotificationPreferencesResponse(
        Map<String, Boolean> inApp,
        Map<String, Boolean> email,
        Map<String, Boolean> push) {
}
