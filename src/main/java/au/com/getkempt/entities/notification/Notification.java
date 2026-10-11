package au.com.getkempt.entities.notification;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.notification.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** An in-app bell notification for one staff user. */
@Document(collection = "notifications")
@Getter
@Setter
public class Notification extends BaseDocument {

    @Indexed
    private String recipientUserId;

    private NotificationType type;
    private String title;
    private String body;
    private String href;
    private boolean read;
}
