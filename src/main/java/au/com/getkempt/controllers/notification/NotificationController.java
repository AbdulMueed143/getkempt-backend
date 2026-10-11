package au.com.getkempt.controllers.notification;

import au.com.getkempt.enums.notification.NotificationType;
import au.com.getkempt.request.notification.NotificationPreferencesRequest;
import au.com.getkempt.request.notification.PushDeviceRegisterRequest;
import au.com.getkempt.responses.notification.NotificationPageResponse;
import au.com.getkempt.responses.notification.NotificationPreferencesResponse;
import au.com.getkempt.responses.notification.PushDeviceResponse;
import au.com.getkempt.services.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** {@code /api/v1/notifications} — see {@code paths/notifications.yaml}. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationPageResponse listNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type) {
        return notificationService.list(cursor, limit, unreadOnly, type);
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markNotificationRead(@PathVariable String notificationId) {
        notificationService.markRead(notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllNotificationsRead() {
        notificationService.markAllRead();
    }

    @GetMapping("/devices")
    public List<PushDeviceResponse> listPushDevices() {
        return notificationService.listDevices();
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public PushDeviceResponse registerPushDevice(@Valid @RequestBody PushDeviceRegisterRequest request) {
        return notificationService.registerDevice(request);
    }

    @DeleteMapping("/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterPushDevice(@PathVariable String deviceId) {
        notificationService.unregisterDevice(deviceId);
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse getNotificationPreferences() {
        return notificationService.getPreferences();
    }

    @PutMapping("/preferences")
    public NotificationPreferencesResponse updateNotificationPreferences(@RequestBody NotificationPreferencesRequest request) {
        return notificationService.updatePreferences(request);
    }
}
