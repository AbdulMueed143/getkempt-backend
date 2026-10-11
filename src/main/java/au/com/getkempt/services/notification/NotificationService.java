package au.com.getkempt.services.notification;

import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.entities.notification.Notification;
import au.com.getkempt.entities.notification.NotificationPreferences;
import au.com.getkempt.entities.notification.PushDevice;
import au.com.getkempt.enums.notification.NotificationType;
import au.com.getkempt.mappers.NotificationMapper;
import au.com.getkempt.repositories.notification.NotificationPreferencesRepository;
import au.com.getkempt.repositories.notification.NotificationRepository;
import au.com.getkempt.repositories.notification.PushDeviceRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.request.notification.NotificationPreferencesRequest;
import au.com.getkempt.request.notification.PushDeviceRegisterRequest;
import au.com.getkempt.responses.notification.NotificationPageResponse;
import au.com.getkempt.responses.notification.NotificationPreferencesResponse;
import au.com.getkempt.responses.notification.NotificationResponse;
import au.com.getkempt.responses.notification.PushDeviceResponse;
import au.com.getkempt.services.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * The in-app bell + push device registry. {@link #notifyUser}/{@link #notifyRole}
 * are the hooks other domains (Bookings, Payments, ...) call to raise a bell
 * notification — see {@code BookingService#createBooking}/{@code #cancel} for
 * the first callers, replacing what used to be log-only comments there.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Set<String> SORTABLE = Set.of("createdAt");

    private final NotificationRepository notificationRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final StaffRepository staffRepository;
    private final NotificationMapper mapper;
    private final OutboxService outboxService;

    // ── bell ────────────────────────────────────────────────────────────────

    public NotificationPageResponse list(String cursor, Integer limit, Boolean unreadOnly, NotificationType type) {
        String userId = currentUserId();
        Criteria criteria = Criteria.where("recipientUserId").is(userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            criteria = criteria.and("read").is(false);
        }
        if (type != null) {
            criteria = criteria.and("type").is(type);
        }
        var page = notificationRepository.list(criteria, SortRequest.parse(null, SORTABLE, SortRequest.desc("createdAt")),
                CursorPageRequest.of(cursor, limit));
        long unreadCount = notificationRepository.count(Criteria.where("recipientUserId").is(userId).and("read").is(false));
        List<NotificationResponse> data = page.data().stream().map(mapper::toResponse).toList();
        return new NotificationPageResponse(data, page.pageInfo(), unreadCount);
    }

    public void markRead(String notificationId) {
        Notification notification = notificationRepository.requireById(notificationId);
        if (!notification.getRecipientUserId().equals(currentUserId())) {
            throw NotFoundException.of("Notification", notificationId);
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllRead() {
        notificationRepository.markAllRead(currentUserId());
    }

    /**
     * Raises a bell notification for one user. Used internally by other domains.
     *
     * <p>Also fans out to the user's registered push devices — per
     * {@code docs/07-notifications.md §3 "Delivery"}, step 2 ("if the user has
     * push devices registered AND {@code NotificationPreferences.push.<type>=true},
     * push to each device") — which previously wasn't implemented at all (the bell
     * row was inserted but nothing was ever actually pushed). The send itself is
     * enqueued through {@link OutboxService} rather than called inline, so a slow
     * push provider can never add latency here either. A type with no preference
     * row yet defaults to enabled (opt-out, not opt-in), matching the docs' framing
     * of preferences as toggles a user turns OFF.
     */
    public void notifyUser(String userId, NotificationType type, String title, String body, String href) {
        if (userId == null) {
            return;
        }
        NotificationPreferences prefs = preferencesRepository.getOrCreate(userId);

        // In-app and push are independent channels (docs/07-notifications.md §3),
        // each an opt-out toggle (a type with no row yet defaults to enabled).
        // Previously the bell row was inserted unconditionally, so a user who
        // turned a type OFF for the bell still saw it; now the in-app toggle is
        // honored, symmetric with push (which could already be muted separately).
        String notificationId = null;
        if (prefs.getInApp().getOrDefault(type.value(), true)) {
            Notification notification = new Notification();
            notification.setRecipientUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setBody(body);
            notification.setHref(href);
            notification.setRead(false);
            notificationRepository.insert(notification);
            notificationId = notification.getId();
        }

        if (prefs.getPush().getOrDefault(type.value(), true)) {
            String shopId = TenantContextHolder.current().map(au.com.getkempt.common.tenant.TenantContext::shopId).orElse(null);
            for (PushDevice device : pushDeviceRepository.findByUserId(userId)) {
                outboxService.enqueuePush(shopId, device.getToken(), title, body, "notification." + type.value(), notificationId);
            }
        }
    }

    /** Raises a bell notification for every staff member holding {@code role} (e.g. the owner). */
    public void notifyRole(StaffRole role, NotificationType type, String title, String body, String href) {
        for (StaffMember staff : staffRepository.findByRole(role)) {
            notifyUser(staff.getUserId(), type, title, body, href);
        }
    }

    // ── push devices ────────────────────────────────────────────────────────

    public List<PushDeviceResponse> listDevices() {
        return pushDeviceRepository.findByUserId(currentUserId()).stream().map(mapper::toResponse).toList();
    }

    public PushDeviceResponse registerDevice(PushDeviceRegisterRequest request) {
        String userId = currentUserId();
        PushDevice device = pushDeviceRepository.findByUserIdAndToken(userId, request.token()).orElseGet(PushDevice::new);
        device.setUserId(userId);
        device.setToken(request.token());
        device.setPlatform(request.platform());
        device.setAppVersion(request.appVersion());
        device.setLastSeenAt(Instant.now());
        if (device.getId() == null) {
            pushDeviceRepository.insert(device);
        } else {
            pushDeviceRepository.save(device);
        }
        return mapper.toResponse(device);
    }

    public void unregisterDevice(String deviceId) {
        PushDevice device = pushDeviceRepository.requireById(deviceId);
        if (!device.getUserId().equals(currentUserId())) {
            throw NotFoundException.of("PushDevice", deviceId);
        }
        pushDeviceRepository.softDelete(device);
    }

    // ── preferences ─────────────────────────────────────────────────────────

    public NotificationPreferencesResponse getPreferences() {
        return mapper.toResponse(preferencesRepository.getOrCreate(currentUserId()));
    }

    public NotificationPreferencesResponse updatePreferences(NotificationPreferencesRequest request) {
        NotificationPreferences prefs = preferencesRepository.getOrCreate(currentUserId());
        if (request.inApp() != null) {
            prefs.setInApp(request.inApp());
        }
        if (request.email() != null) {
            prefs.setEmail(request.email());
        }
        if (request.push() != null) {
            prefs.setPush(request.push());
        }
        preferencesRepository.save(prefs);
        return mapper.toResponse(prefs);
    }

    private String currentUserId() {
        return TenantContextHolder.require().userId();
    }
}
