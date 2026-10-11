package au.com.getkempt.entities.notification;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.notification.PushDevicePlatform;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A registered push token for one staff user's device. */
@Document(collection = "push_devices")
@Getter
@Setter
public class PushDevice extends BaseDocument {

    @Indexed
    private String userId;

    private String token;
    private PushDevicePlatform platform;
    private String appVersion;
    private Instant lastSeenAt;
}
