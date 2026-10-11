package au.com.getkempt.mappers;

import au.com.getkempt.entities.notification.Notification;
import au.com.getkempt.entities.notification.NotificationPreferences;
import au.com.getkempt.entities.notification.PushDevice;
import au.com.getkempt.responses.notification.NotificationPreferencesResponse;
import au.com.getkempt.responses.notification.NotificationResponse;
import au.com.getkempt.responses.notification.PushDeviceResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getHref(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    public PushDeviceResponse toResponse(PushDevice device) {
        return new PushDeviceResponse(
                device.getId(),
                device.getToken(),
                device.getPlatform(),
                device.getAppVersion(),
                device.getLastSeenAt(),
                device.getCreatedAt());
    }

    public NotificationPreferencesResponse toResponse(NotificationPreferences prefs) {
        return new NotificationPreferencesResponse(prefs.getInApp(), prefs.getEmail(), prefs.getPush());
    }
}
