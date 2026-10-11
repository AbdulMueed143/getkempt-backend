package au.com.getkempt.responses.notification;

import au.com.getkempt.enums.notification.PushDevicePlatform;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PushDeviceResponse(
        String id,
        String token,
        PushDevicePlatform platform,
        String appVersion,
        Instant lastSeenAt,
        Instant createdAt) {
}
