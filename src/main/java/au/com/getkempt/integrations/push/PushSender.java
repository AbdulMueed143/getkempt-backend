package au.com.getkempt.integrations.push;

import java.util.Map;

/**
 * Port for push notifications (Expo / APNs / FCM in production). Used by the in-app
 * notification system when a user has registered push devices.
 */
public interface PushSender {

    record Result(String providerMessageId) {
    }

    Result send(String deviceToken, String title, String body, Map<String, String> data);
}
