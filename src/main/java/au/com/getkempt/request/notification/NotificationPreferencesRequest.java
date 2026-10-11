package au.com.getkempt.request.notification;

import java.util.Map;

public record NotificationPreferencesRequest(
        Map<String, Boolean> inApp,
        Map<String, Boolean> email,
        Map<String, Boolean> push) {
}
