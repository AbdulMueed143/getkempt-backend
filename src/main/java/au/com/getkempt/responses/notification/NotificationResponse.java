package au.com.getkempt.responses.notification;

import au.com.getkempt.enums.notification.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        String id,
        NotificationType type,
        String title,
        String body,
        String href,
        boolean read,
        Instant createdAt) {
}
