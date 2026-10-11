package au.com.getkempt.request.notification;

import au.com.getkempt.enums.notification.PushDevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushDeviceRegisterRequest(
        @NotBlank String token,
        @NotNull PushDevicePlatform platform,
        String appVersion) {
}
