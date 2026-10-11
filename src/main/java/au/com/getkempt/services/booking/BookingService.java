package au.com.getkempt.services.booking;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContext;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.configurations.security.properties.BookingProperties;
import au.com.getkempt.entities.Client;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.entities.availability.TimeSlot;
import au.com.getkempt.entities.booking.AppliedSurcharge;
import au.com.getkempt.entities.booking.AppointmentRecord;
import au.com.getkempt.entities.booking.Booking;
import au.com.getkempt.entities.booking.SlotLock;
import au.com.getkempt.enums.booking.AppointmentStatus;
import au.com.getkempt.entities.store.Holiday;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.entities.store.SurchargeRule;
import au.com.getkempt.enums.booking.BookingSource;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.enums.booking.CancellationReason;
import au.com.getkempt.enums.store.DepositType;
import au.com.getkempt.enums.store.PaymentMode;
import au.com.getkempt.enums.store.SurchargeDay;
import au.com.getkempt.enums.store.SurchargeType;
import au.com.getkempt.integrations.payment.PaymentGateway;
import au.com.getkempt.mappers.BookingMapper;
import au.com.getkempt.mappers.StoreMapper;
import au.com.getkempt.repositories.booking.AppointmentRecordRepository;
import au.com.getkempt.repositories.booking.BookingRepository;
import au.com.getkempt.repositories.booking.SlotLockRepository;
import au.com.getkempt.repositories.client.ClientRepository;
import au.com.getkempt.repositories.pkg.PackageRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.repositories.store.StoreSettingsRepository;
import au.com.getkempt.request.booking.BookingBulkRequest;
import au.com.getkempt.request.booking.BookingCreateRequest;
import au.com.getkempt.request.booking.BookingNotesRequest;
import au.com.getkempt.request.booking.BookingUpdateRequest;
import au.com.getkempt.request.booking.CancelRequest;
import au.com.getkempt.request.booking.CompleteBookingRequest;
import au.com.getkempt.request.booking.ConflictCheckRequest;
import au.com.getkempt.request.booking.InlineClientRequest;
import au.com.getkempt.request.booking.NoShowRequest;
import au.com.getkempt.request.booking.RescheduleRequest;
import au.com.getkempt.request.booking.SlotSearchRequest;
import au.com.getkempt.responses.booking.AvailableSlotResponse;
import au.com.getkempt.responses.booking.BookingBulkResponse;
import au.com.getkempt.responses.booking.BookingCreateResponse;
import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.responses.booking.CalendarResponse;
import au.com.getkempt.responses.booking.ConflictCheckResponse;
import au.com.getkempt.responses.booking.SlotSearchResponse;
import au.com.getkempt.responses.payment.PaymentIntentResponse;
import au.com.getkempt.services.availability.AvailabilityService;
import au.com.getkempt.services.loyalty.LoyaltyService;
import au.com.getkempt.services.notification.NotificationService;
import au.com.getkempt.services.outbox.OutboxService;
import au.com.getkempt.services.pkg.PackagePricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bookings — the core appointment lifecycle, per {@code docs/05-booking-lifecycle.md}.
 *
 * <p>Background jobs the doc also describes (auto in-progress/no-show transitions,
 * pending-payment expiry) still need dedicated polling logic this class doesn't
 * implement — every state transition here is instead driven by its explicit
 * endpoint, exactly as the contract's dedicated-endpoint-per-transition design
 * already assumes. (A scheduler now exists in the codebase — see {@code
 * OutboxDispatcher} — but it drains the notification outbox, not these
 * booking-lifecycle jobs; a future worker could reuse the same {@code
 * @Scheduled} infrastructure for them.) Client-facing lifecycle notifications
 * (confirmed/cancelled/rescheduled) ARE implemented, via {@link OutboxService}
 * — see {@link #notifyClientOfBookingEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<String> SORTABLE = Set.of("startAt", "createdAt");

    private final BookingRepository bookingRepository;
    private final AppointmentRecordRepository appointmentRecordRepository;
    private final SlotLockRepository slotLockRepository;
    private final ClientRepository clientRepository;
    private final PackageRepository packageRepository;
    private final StaffRepository staffRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final AvailabilityService availabilityService;
    private final LoyaltyService loyaltyService;
    private final NotificationService notificationService;
    private final OutboxService outboxService;
    private final PaymentGateway paymentGateway;
    private final au.com.getkempt.services.payment.PaymentMethodPolicy paymentMethodPolicy;
    private final PackagePricingService packagePricingService;
    private final BookingMapper bookingMapper;
    private final StoreMapper storeMapper;
    private final BookingProperties bookingProperties;
    private final au.com.getkempt.repositories.service.ServiceRepository serviceRepository;

    // ── collection ──────────────────────────────────────────────────────────

    public CursorPage<BookingResponse> list(String cursor, Integer limit, String sort,
                                            LocalDate fromDate, LocalDate toDate,
                                            String staffId, String clientId, String serviceId,
                                            List<BookingStatus> status, BookingSource source) {
        TenantContext ctx = TenantContextHolder.require();
        List<Criteria> ands = new ArrayList<>();
        if (staffId != null) {
            ands.add(Criteria.where("staffId").is(staffId));
        } else if (!ctx.hasPermission(Permission.BOOKINGS_READ_ALL) && !ctx.isOwner()) {
            // Plain "staff" role has no read-all permission — scope the list to
            // their own bookings rather than 403ing the whole endpoint.
            ands.add(Criteria.where("staffId").is(ctx.staffId()));
        }
        if (clientId != null) {
            ands.add(Criteria.where("clientId").is(clientId));
        }
        if (serviceId != null) {
            ands.add(Criteria.where("serviceId").is(serviceId));
        }
        if (status != null && !status.isEmpty()) {
            ands.add(Criteria.where("status").in(status));
        }
        if (source != null) {
            ands.add(Criteria.where("source").is(source));
        }
        if (fromDate != null) {
            ands.add(Criteria.where("startAt").gte(fromDate.atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        if (toDate != null) {
            ands.add(Criteria.where("startAt").lt(toDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        Criteria filter = ands.isEmpty() ? new Criteria() : new Criteria().andOperator(ands.toArray(new Criteria[0]));

        SortRequest sortRequest = SortRequest.parse(sort, SORTABLE, SortRequest.desc("startAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return bookingRepository.list(filter, sortRequest, page).map(bookingMapper::toResponse);
    }

    public BookingResponse get(String bookingId) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanSee(booking);
        return bookingMapper.toResponse(booking);
    }

    // ── create ──────────────────────────────────────────────────────────────

    /**
     * Transactional per {@code docs/05-booking-lifecycle.md}: slot validation, the
     * {@link SlotLock} bucket inserts, and the booking write must commit atomically,
     * or a losing concurrent request could observe a half-applied state (locks
     * held by a booking that never got persisted, or vice versa). Requires Mongo
     * running as a replica set — see {@code MongoTransactionConfig}.
     */
    @Transactional("mongoTransactionManager")
    public BookingCreateResponse createBooking(BookingCreateRequest request) {
        TenantContext ctx = requireCanCreateFor(request.staffId());
        StaffMember staff = staffRepository.requireById(request.staffId());

        if ((request.serviceId() == null) == (request.packageId() == null)) {
            throw new UnprocessableEntityException("Exactly one of serviceId or packageId is required");
        }

        Snapshot snapshot = resolveSnapshot(request.serviceId(), request.packageId(), staff);
        Client client = resolveClient(request.clientId(), request.client());

        Instant startAt = request.startAt();
        Instant endAt = startAt.plusSeconds(snapshot.durationMinutes() * 60L);
        String timezone = availabilityService.timezoneFor(staff.getId());
        ZonedDateTime localStart = startAt.atZone(ZoneId.of(timezone));

        boolean ownerOverride = Boolean.TRUE.equals(request.skipConflictCheck()) && ctx.isOwner();
        StoreSettings settings = requireSettings();

        if (!ownerOverride) {
            List<ConflictCheckResponse.Conflict> conflicts = findConflicts(staff.getId(), startAt, endAt, null, settings, timezone);
            if (!conflicts.isEmpty()) {
                throw new ConflictException("The requested slot is not available: "
                        + conflicts.stream().map(ConflictCheckResponse.Conflict::label).toList());
            }
        }

        String bookingId = IdGenerator.generate(IdPrefix.BOOKING);
        if (!ownerOverride) {
            acquireSlotLocks(bookingId, staff.getId(), startAt, endAt);
        }

        List<AppliedSurcharge> surcharges = computeSurcharges(settings, staff.getId(), localStart, snapshot.price());
        Money total = snapshot.price();
        for (AppliedSurcharge s : surcharges) {
            total = total.plus(s.getAmount());
        }

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStaffId(staff.getId());
        booking.setStaffName(staff.getFirstName() + " " + staff.getLastName());
        booking.setClientId(client.getId());
        booking.setClientName(client.getFirstName() + " " + client.getLastName());
        booking.setClientPhone(client.getPhone());
        booking.setClientEmail(client.getEmail());
        booking.setServiceId(snapshot.serviceId());
        booking.setPackageId(snapshot.packageId());
        booking.setServiceName(snapshot.name());
        booking.setDurationMinutes(snapshot.durationMinutes());
        booking.setPrice(total);
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        booking.setSource(request.source() != null ? request.source() : BookingSource.MANUAL);
        booking.setNotes(request.notes());
        booking.setSurchargesApplied(surcharges);

        PaymentIntentResponse paymentIntentResponse = null;
        PaymentMode paymentMode = settings.getPaymentMode() == null ? PaymentMode.IN_STORE : settings.getPaymentMode();
        // A shop can only actually collect an online payment once Stripe Connect
        // onboarding has completed — otherwise createIntent() would mint a
        // PaymentIntent with nowhere for the funds to land. IN_STORE never hits
        // the gateway at all, so it's unaffected.
        if (paymentMode != PaymentMode.IN_STORE) {
            paymentMethodPolicy.requireOnlineCapable(settings);
        }
        switch (paymentMode) {
            case IN_STORE -> booking.setStatus(BookingStatus.CONFIRMED);
            case DEPOSIT -> {
                Money deposit = computeDeposit(settings, total);
                PaymentGateway.IntentResult result = paymentGateway.createIntent(new PaymentGateway.CreateIntent(
                        deposit, "deposit", false, Map.of("bookingId", bookingId, "shopId", TenantContextHolder.requireShopId())));
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentIntentId(result.intentId());
                paymentIntentResponse = toPaymentIntentResponse(result, deposit, bookingId, client.getId());
            }
            case FULL_UPFRONT -> {
                PaymentGateway.IntentResult result = paymentGateway.createIntent(new PaymentGateway.CreateIntent(
                        total, "full_payment", false, Map.of("bookingId", bookingId, "shopId", TenantContextHolder.requireShopId())));
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentIntentId(result.intentId());
                paymentIntentResponse = toPaymentIntentResponse(result, total, bookingId, client.getId());
            }
        }

        bookingRepository.insert(booking);

        AppointmentRecord appointment = new AppointmentRecord();
        appointment.setBookingId(bookingId);
        appointment.setClientId(client.getId());
        appointment.setClientName(booking.getClientName());
        appointment.setServiceName(booking.getServiceName());
        appointment.setDurationMinutes(booking.getDurationMinutes());
        appointment.setStaffId(staff.getId());
        appointment.setStaffName(booking.getStaffName());
        appointment.setScheduledAt(startAt);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointmentRecordRepository.insert(appointment);

        // Denormalised counter kept in sync here; totalBookings/lifetimeSpend only
        // change on completion (docs step 4 of the "Completion flow").
        if (client.getNextBookingDate() == null || startAt.isBefore(client.getNextBookingDate())) {
            client.setNextBookingDate(startAt);
            clientRepository.save(client);
        }

        // Full reminder scheduling (docs step 9 — timed appointment_reminder /
        // appointment_followup rules, per-shop templates and toggles) needs the
        // Reminders domain, which doesn't exist yet. The simpler, always-fire
        // "booking_confirmed" client notification below IS implemented — via the
        // Outbox, so it never blocks this response — and is written so the real
        // Reminders domain can eventually call the same OutboxService methods
        // instead of duplicating send logic.
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            notifyClientOfBookingEvent(booking, "booking.confirmed",
                    "Your appointment is confirmed",
                    "Hi " + client.getFirstName() + ", your " + booking.getServiceName() + " with "
                            + booking.getStaffName() + " is confirmed for " + booking.getStartAt() + ".",
                    "<p>Hi " + client.getFirstName() + ", your <strong>" + booking.getServiceName() + "</strong> with "
                            + booking.getStaffName() + " is confirmed for " + booking.getStartAt() + ".</p>");
        }
        log.info("[booking-create] {} created for shop {} — timed reminder scheduling skipped (Reminders domain not yet implemented)",
                bookingId, TenantContextHolder.requireShopId());

        String bellHref = "/bookings/" + bookingId;
        notificationService.notifyRole(au.com.getkempt.common.security.StaffRole.OWNER, au.com.getkempt.enums.notification.NotificationType.NEW_BOOKING,
                "New booking", booking.getClientName() + " booked " + booking.getServiceName(), bellHref);
        if (staff.getUserId() != null) {
            notificationService.notifyUser(staff.getUserId(), au.com.getkempt.enums.notification.NotificationType.NEW_BOOKING,
                    "New booking", booking.getClientName() + " booked " + booking.getServiceName(), bellHref);
        }

        return new BookingCreateResponse(bookingMapper.toResponse(booking), paymentIntentResponse);
    }

    // ── state transitions ──────────────────────────────────────────────────

    public BookingResponse update(String bookingId, String ifMatch, BookingUpdateRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        IfMatchSupport.check(ifMatch, booking.getVersion());

        if (request.staffId() != null || request.startAt() != null) {
            throw new BadRequestException("Use POST /bookings/{id}/reschedule to change staff or time");
        }
        if (!activeStatuses().contains(booking.getStatus())) {
            throw new ConflictException("Cannot edit a " + booking.getStatus().value() + " booking");
        }
        if (request.serviceId() != null && !request.serviceId().equals(booking.getServiceId())) {
            au.com.getkempt.entities.Service service = requireServiceForStaff(request.serviceId(), booking.getStaffId());
            int newDuration = service.getDurationMinutes() + service.getBufferMinutes();
            Instant newEnd = booking.getStartAt().plusSeconds(newDuration * 60L);
            List<ConflictCheckResponse.Conflict> conflicts = findConflicts(
                    booking.getStaffId(), booking.getStartAt(), newEnd, bookingId, requireSettings(),
                    availabilityService.timezoneFor(booking.getStaffId()));
            if (!conflicts.isEmpty()) {
                throw new ConflictException("New service duration no longer fits the booked slot");
            }
            booking.setServiceId(service.getId());
            booking.setServiceName(service.getName());
            booking.setDurationMinutes(newDuration);
            booking.setEndAt(newEnd);
            reacquireSlotLocks(bookingId, booking.getStaffId(), booking.getStartAt(), newEnd);
        }
        if (request.notes() != null) {
            booking.setNotes(request.notes());
        }
        bookingRepository.save(booking);
        return bookingMapper.toResponse(booking);
    }

    public void delete(String bookingId) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        if (booking.getStatus() != BookingStatus.PENDING
                || booking.getCreatedAt().isBefore(Instant.now().minusSeconds(5 * 60))) {
            throw new ConflictException("Only a pending booking created in the last 5 minutes can be deleted; cancel it instead");
        }
        slotLockRepository.releaseAll(bookingId);
        bookingRepository.softDelete(booking);
    }

    @Transactional("mongoTransactionManager")
    public BookingResponse cancel(String bookingId, CancelRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCancelAccess();
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Only a pending or confirmed booking can be cancelled");
        }

        StoreSettings settings = requireSettings();
        if (Boolean.TRUE.equals(request.chargeFee())) {
            applyCancellationFee(booking, settings);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.reason());
        booking.setCancellationNote(request.note());
        slotLockRepository.releaseAll(bookingId);
        bookingRepository.save(booking);
        updateAppointmentStatus(bookingId, AppointmentStatus.CANCELLED, request.note());

        notifyClientOfBookingEvent(booking, "booking.cancelled",
                "Your appointment was cancelled",
                "Hi, your " + booking.getServiceName() + " with " + booking.getStaffName()
                        + " on " + booking.getStartAt() + " was cancelled.",
                "<p>Your <strong>" + booking.getServiceName() + "</strong> with " + booking.getStaffName()
                        + " on " + booking.getStartAt() + " was cancelled.</p>");
        notificationService.notifyRole(au.com.getkempt.common.security.StaffRole.OWNER, au.com.getkempt.enums.notification.NotificationType.BOOKING_CANCELLED,
                "Booking cancelled", booking.getClientName() + "'s booking was cancelled", "/bookings/" + bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Transactional("mongoTransactionManager")
    public BookingResponse reschedule(String bookingId, RescheduleRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        String targetStaffId = request.newStaffId() != null ? request.newStaffId() : booking.getStaffId();
        requireCanCreateFor(targetStaffId);
        requireCanWrite(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Only a confirmed or in-progress booking can be rescheduled");
        }

        StaffMember targetStaff = staffRepository.requireById(targetStaffId);
        Instant newStart = request.newStartAt();
        Instant newEnd = newStart.plusSeconds(booking.getDurationMinutes() * 60L);
        StoreSettings settings = requireSettings();
        String timezone = availabilityService.timezoneFor(targetStaffId);

        List<ConflictCheckResponse.Conflict> conflicts = findConflicts(targetStaffId, newStart, newEnd, bookingId, settings, timezone);
        if (!conflicts.isEmpty()) {
            throw new ConflictException("The new slot is not available: " + conflicts.stream().map(ConflictCheckResponse.Conflict::label).toList());
        }

        slotLockRepository.releaseAll(bookingId);
        acquireSlotLocks(bookingId, targetStaffId, newStart, newEnd);

        Money previousPrice = booking.getPrice();
        List<AppliedSurcharge> newSurcharges = computeSurcharges(settings, targetStaffId, newStart.atZone(ZoneId.of(timezone)),
                previousPrice.minus(sumSurcharges(booking.getSurchargesApplied())));
        Money newBase = previousPrice.minus(sumSurcharges(booking.getSurchargesApplied()));
        Money newTotal = newBase;
        for (AppliedSurcharge s : newSurcharges) {
            newTotal = newTotal.plus(s.getAmount());
        }

        booking.setStaffId(targetStaffId);
        booking.setStaffName(targetStaff.getFirstName() + " " + targetStaff.getLastName());
        booking.setStartAt(newStart);
        booking.setEndAt(newEnd);
        booking.setSurchargesApplied(newSurcharges);
        if (newTotal.amountCents() != previousPrice.amountCents()) {
            // Price delta noted, not charged: actually collecting the difference
            // needs the Payments domain (capture/charge an incremental amount),
            // which doesn't exist yet — see class javadoc.
            log.info("[booking-reschedule] {} price changed {} -> {} — not auto-charged (Payments domain not yet implemented)",
                    bookingId, previousPrice.amountCents(), newTotal.amountCents());
        }
        booking.setPrice(newTotal);
        bookingRepository.save(booking);
        appointmentRecordRepository.findByBookingId(bookingId).ifPresent(record -> {
            record.setScheduledAt(newStart);
            record.setStaffId(targetStaffId);
            record.setStaffName(booking.getStaffName());
            appointmentRecordRepository.save(record);
        });

        notifyClientOfBookingEvent(booking, "booking.rescheduled",
                "Your appointment was rescheduled",
                "Hi, your " + booking.getServiceName() + " with " + booking.getStaffName()
                        + " has moved to " + newStart + ".",
                "<p>Your <strong>" + booking.getServiceName() + "</strong> with " + booking.getStaffName()
                        + " has moved to " + newStart + ".</p>");
        return bookingMapper.toResponse(booking);
    }

    public BookingResponse confirm(String bookingId) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Only a pending booking can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        // This is the deposit/full_upfront path's "booking_confirmed" trigger — the
        // IN_STORE path fires the equivalent notification inline in createBooking
        // since that path is confirmed immediately. In production this transition
        // would instead be driven by the `payment_intent.succeeded` Stripe webhook
        // (see docs/06-payments.md); this explicit endpoint stands in for that
        // until the Webhooks domain exists.
        notifyClientOfBookingEvent(booking, "booking.confirmed",
                "Your appointment is confirmed",
                "Hi, your " + booking.getServiceName() + " with " + booking.getStaffName()
                        + " is confirmed for " + booking.getStartAt() + ".",
                "<p>Your <strong>" + booking.getServiceName() + "</strong> with " + booking.getStaffName()
                        + " is confirmed for " + booking.getStartAt() + ".</p>");
        return bookingMapper.toResponse(booking);
    }

    /**
     * Transactional: completion fans out into the {@code AppointmentRecord}
     * status, the client's denormalised counters, AND (via {@link LoyaltyService#advanceProgress})
     * potentially a new {@code LoyaltyReward} plus advancing the client to the
     * next program — several collections that must all move together, or a
     * client could end up with incremented spend but no corresponding loyalty
     * credit (or vice versa).
     */
    @Transactional("mongoTransactionManager")
    public BookingResponse complete(String bookingId, CompleteBookingRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Only a confirmed or in-progress booking can be completed");
        }
        if (booking.getPaymentIntentId() != null && !booking.isPaymentCaptured()) {
            paymentGateway.capture(booking.getPaymentIntentId());
            booking.setPaymentCaptured(true);
        } else if (request != null && request.inStoreMethod() != null) {
            // No online PaymentIntent (paymentMode=in_store, or the fee was
            // otherwise never collected via the gateway) — record which allowed
            // in-store method the staff actually took, per the shop's own
            // StoreSettings.inStoreMethods allow-list. Rejecting a disallowed
            // method here (rather than accepting anything) is the concrete form
            // of "stores select which payment methods are allowed".
            requireAllowedInStoreMethod(request.inStoreMethod());
            booking.setInStoreMethodUsed(request.inStoreMethod());
        }
        if (request != null && request.actualEndAt() != null) {
            booking.setEndAt(request.actualEndAt());
        }
        booking.setStatus(BookingStatus.COMPLETED);
        // Release the per-bucket slot locks now the appointment is done. Overlap
        // queries already ignore non-active bookings, so a leaked lock here would
        // otherwise falsely 409 a legitimate new booking that reuses the bucket
        // (e.g. after an early finish) — matching the cancel/no-show/delete paths.
        slotLockRepository.releaseAll(bookingId);
        bookingRepository.save(booking);
        updateAppointmentStatus(bookingId, AppointmentStatus.COMPLETED, null);

        Client client = clientRepository.findById(booking.getClientId()).orElse(null);
        if (client != null) {
            client.setTotalBookings(client.getTotalBookings() + 1);
            long spend = booking.getPrice().amountCents() + (request != null && request.tip() != null ? request.tip().amountCents() : 0);
            client.setLifetimeSpendCents(client.getLifetimeSpendCents() + spend);
            client.setLastBookingDate(booking.getEndAt());
            if (booking.getStartAt().equals(client.getNextBookingDate())) {
                client.setNextBookingDate(null); // recomputing the true next booking needs a query the Bookings/Clients boundary doesn't currently expose both ways; cleared rather than left stale
            }
            clientRepository.save(client);
            loyaltyService.advanceProgress(client, booking);
        }

        return bookingMapper.toResponse(booking);
    }

    public BookingResponse noShow(String bookingId, NoShowRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Only a confirmed or in-progress booking can be marked no-show");
        }
        StoreSettings settings = requireSettings();
        boolean chargeFee = request != null && request.chargeFee() != null ? request.chargeFee() : settings.isAutoCharge();
        if (chargeFee && settings.getNoShowFeeType() != null && settings.getNoShowFeeType() != au.com.getkempt.enums.store.FeeType.NONE) {
            Money fee = feeAmount(settings.getNoShowFeeType(), settings.getNoShowFeeValue(), booking.getPrice());
            if (booking.getPaymentIntentId() != null) {
                paymentGateway.capture(booking.getPaymentIntentId());
            } else {
                PaymentGateway.IntentResult result = paymentGateway.createIntent(new PaymentGateway.CreateIntent(
                        fee, "no_show_fee", false, Map.of("bookingId", bookingId)));
                booking.setPaymentIntentId(result.intentId());
            }
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setCancellationNote(request != null ? request.note() : null);
        slotLockRepository.releaseAll(bookingId);
        bookingRepository.save(booking);
        updateAppointmentStatus(bookingId, AppointmentStatus.NO_SHOW, request != null ? request.note() : null);
        return bookingMapper.toResponse(booking);
    }

    public BookingResponse setNotes(String bookingId, BookingNotesRequest request) {
        Booking booking = bookingRepository.requireById(bookingId);
        requireCanWrite(booking);
        booking.setNotes(request.notes());
        bookingRepository.save(booking);
        return bookingMapper.toResponse(booking);
    }

    // ── calendar / search / conflicts ───────────────────────────────────────

    public CalendarResponse getCalendar(Instant from, Instant to, List<String> staffIds, boolean includeBreaks) {
        List<Booking> bookings = bookingRepository.findForCalendar(from, to, staffIds);
        List<CalendarResponse.BreakBlock> breaks = new ArrayList<>();
        if (includeBreaks) {
            Set<String> ids = (staffIds != null && !staffIds.isEmpty())
                    ? Set.copyOf(staffIds)
                    : staffRepository.list(new Criteria(), SortRequest.asc("createdAt"), CursorPageRequest.of(null, CursorPageRequest.MAX_LIMIT))
                        .data().stream().map(StaffMember::getId).collect(java.util.stream.Collectors.toSet());
            for (String staffId : ids) {
                breaks.addAll(breaksInRange(staffId, from, to));
            }
        }
        List<au.com.getkempt.entities.store.Holiday> holidays = requireSettings().getHolidays().stream()
                .filter(h -> !h.getDate().isBefore(from.atZone(ZoneId.of("UTC")).toLocalDate())
                        && !h.getDate().isAfter(to.atZone(ZoneId.of("UTC")).toLocalDate()))
                .toList();
        return new CalendarResponse(
                bookings.stream().map(bookingMapper::toResponse).toList(),
                breaks,
                holidays.stream().map(storeMapper::toResponse).toList());
    }

    public SlotSearchResponse searchSlots(SlotSearchRequest request) {
        if ((request.serviceId() == null) == (request.packageId() == null)) {
            throw new UnprocessableEntityException("Exactly one of serviceId or packageId is required");
        }
        StoreSettings settings = requireSettings();
        int interval = request.intervalMinutes() != null ? request.intervalMinutes() : settings.getSlotInterval();
        if (interval <= 0) {
            interval = 15;
        }

        List<StaffMember> qualifiedStaff;
        if (request.serviceId() != null) {
            au.com.getkempt.entities.Service service = serviceRepository.requireById(request.serviceId());
            qualifiedStaff = staffRepository.findByServiceId(service.getId());
        } else {
            au.com.getkempt.entities.Package pkg = packageRepository.requireById(request.packageId());
            qualifiedStaff = staffRepository.findByIds(pkg.getStaffIds());
        }
        List<StaffMember> candidates;
        if (request.staffId() != null) {
            // Narrow to the requested staff member, but only if they actually
            // qualify — an unqualified explicit staffId yields no slots rather
            // than silently ignoring the service/package's staffIds constraint.
            candidates = qualifiedStaff.stream().filter(s -> s.getId().equals(request.staffId())).toList();
        } else {
            candidates = qualifiedStaff;
        }
        Snapshot snapshot = resolveSnapshotForStaffless(request.serviceId(), request.packageId());
        int durationMinutes = snapshot.durationMinutes();
        Money basePrice = snapshot.price();

        List<AvailableSlotResponse> slots = new ArrayList<>();
        for (StaffMember staff : candidates) {
            String timezone = availabilityService.timezoneFor(staff.getId());
            AvailabilityService.EffectiveDay effectiveDay = availabilityService.effectiveDay(staff.getId(), request.date());
            if (!effectiveDay.isWorking()) {
                continue;
            }
            if (isClosedByHoliday(settings, staff.getId(), request.date())) {
                continue;
            }
            Instant dayStart = request.date().atStartOfDay(ZoneId.of(timezone)).toInstant();
            Instant dayEnd = request.date().plusDays(1).atStartOfDay(ZoneId.of(timezone)).toInstant();
            List<Booking> existing = bookingRepository.findActiveForStaffInRange(staff.getId(), dayStart, dayEnd);

            List<TimeSlot> breaks = effectiveDay.breakSlots();
            for (TimeSlot work : effectiveDay.workSlots()) {
                int slotStartMin = minutesOf(work.getStartTime());
                int slotEndMin = minutesOf(work.getEndTime());
                for (int candidateStart = slotStartMin; candidateStart + durationMinutes <= slotEndMin; candidateStart += interval) {
                    if (overlapsBreak(breaks, candidateStart, candidateStart + durationMinutes)) {
                        continue;
                    }
                    Instant candidateStartInstant = request.date().atStartOfDay(ZoneId.of(timezone)).plusMinutes(candidateStart).toInstant();
                    Instant candidateEndInstant = candidateStartInstant.plusSeconds(durationMinutes * 60L);
                    boolean overlaps = existing.stream().anyMatch(b -> b.getStartAt().isBefore(candidateEndInstant) && b.getEndAt().isAfter(candidateStartInstant));
                    if (overlaps) {
                        continue;
                    }
                    ZonedDateTime localStart = candidateStartInstant.atZone(ZoneId.of(timezone));
                    List<AppliedSurcharge> surcharges = computeSurcharges(settings, staff.getId(), localStart, basePrice);
                    Money surchargeTotal = Money.zero();
                    for (AppliedSurcharge s : surcharges) {
                        surchargeTotal = surchargeTotal.plus(s.getAmount());
                    }
                    slots.add(new AvailableSlotResponse(candidateStartInstant, candidateEndInstant, staff.getId(),
                            staff.getFirstName() + " " + staff.getLastName(),
                            surchargeTotal.amountCents() > 0 ? surchargeTotal : null));
                }
            }
        }
        slots.sort((a, b) -> a.startAt().compareTo(b.startAt()));
        return new SlotSearchResponse(slots);
    }

    public ConflictCheckResponse checkConflicts(ConflictCheckRequest request) {
        Instant endAt = request.startAt().plusSeconds(request.durationMinutes() * 60L);
        StoreSettings settings = requireSettings();
        String timezone = availabilityService.timezoneFor(request.staffId());
        List<ConflictCheckResponse.Conflict> conflicts = findConflicts(request.staffId(), request.startAt(), endAt, request.ignoreBookingId(), settings, timezone);
        return new ConflictCheckResponse(conflicts.isEmpty(), conflicts);
    }

    // ── bulk / export / ical ────────────────────────────────────────────────

    public BookingBulkResponse bulk(BookingBulkRequest request) {
        List<String> succeeded = new ArrayList<>();
        List<BookingBulkResponse.Failure> failed = new ArrayList<>();
        for (String bookingId : request.bookingIds()) {
            try {
                switch (request.action()) {
                    case "confirm" -> confirm(bookingId);
                    case "cancel" -> cancel(bookingId, new CancelRequest(
                            CancellationReason.OTHER, request.reason(), false, request.notifyClients()));
                    case "complete" -> complete(bookingId, null);
                    case "no_show" -> noShow(bookingId, new NoShowRequest(request.reason(), null));
                    default -> throw new BadRequestException("Unknown bulk action: " + request.action());
                }
                succeeded.add(bookingId);
            } catch (RuntimeException ex) {
                failed.add(new BookingBulkResponse.Failure(bookingId, ex.getMessage()));
            }
        }
        return new BookingBulkResponse(succeeded, failed);
    }

    public String export(LocalDate from, LocalDate to, List<BookingStatus> statuses) {
        // Same synchronous-stub reasoning as ClientService#export — no async job
        // runner or object-storage write capability exists yet in this codebase.
        requireReadAllAccess();
        List<Criteria> ands = new ArrayList<>();
        if (from != null) {
            ands.add(Criteria.where("startAt").gte(from.atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        if (to != null) {
            ands.add(Criteria.where("startAt").lt(to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        if (statuses != null && !statuses.isEmpty()) {
            ands.add(Criteria.where("status").in(statuses));
        }
        Criteria filter = ands.isEmpty() ? new Criteria() : new Criteria().andOperator(ands.toArray(new Criteria[0]));
        CursorPage<Booking> page = bookingRepository.list(filter, SortRequest.desc("startAt"), CursorPageRequest.of(null, CursorPageRequest.MAX_LIMIT));
        log.info("[booking-export] generated {} rows for shop {}", page.data().size(), TenantContextHolder.requireShopId());
        return IdGenerator.generate(IdPrefix.REQUEST);
    }

    public String getIcalFeedUrl(String staffId) {
        TenantContext ctx = TenantContextHolder.require();
        String targetStaffId = staffId != null ? staffId : ctx.staffId();
        if (targetStaffId == null) {
            throw new BadRequestException("staffId is required when the caller has no linked staff record");
        }
        staffRepository.requireById(targetStaffId);
        // No ICS-serving endpoint exists in the contract (only this URL-minting one),
        // so a real per-user signed token scheme (Tokens.randomToken(), stored and
        // verified server-side) is future work once that consumer exists.
        String token = au.com.getkempt.common.security.Tokens.randomToken();
        return "https://api.getkempt.co/api/v1/bookings/ical/%s?token=%s".formatted(targetStaffId, token);
    }

    // ── conflict detection (shared by create / reschedule / conflicts-check) ──

    private List<ConflictCheckResponse.Conflict> findConflicts(String staffId, Instant startAt, Instant endAt,
                                                               String ignoreBookingId, StoreSettings settings, String timezone) {
        List<ConflictCheckResponse.Conflict> conflicts = new ArrayList<>();
        ZonedDateTime localStart = startAt.atZone(ZoneId.of(timezone));
        LocalDate localDate = localStart.toLocalDate();

        ZonedDateTime localEnd = endAt.atZone(ZoneId.of(timezone));
        AvailabilityService.EffectiveDay effectiveDay = availabilityService.effectiveDay(staffId, localDate);
        if (!effectiveDay.isWorking() || !fitsWithinWorkSlots(effectiveDay.workSlots(), localStart, localEnd)) {
            conflicts.add(new ConflictCheckResponse.Conflict("outside_hours", startAt, endAt, "Outside working hours"));
        } else if (localStart.toLocalDate().equals(localEnd.toLocalDate())
                && overlapsBreak(effectiveDay.breakSlots(),
                        localStart.getHour() * 60 + localStart.getMinute(),
                        localEnd.getHour() * 60 + localEnd.getMinute())) {
            conflicts.add(new ConflictCheckResponse.Conflict("break", startAt, endAt, "Overlaps a scheduled break"));
        }
        if (isClosedByHoliday(settings, staffId, localDate)) {
            conflicts.add(new ConflictCheckResponse.Conflict("holiday", startAt, endAt, "Shop closed for a holiday"));
        }
        for (Booking existing : bookingRepository.findOverlapping(staffId, startAt, endAt, ignoreBookingId)) {
            conflicts.add(new ConflictCheckResponse.Conflict("booking", existing.getStartAt(), existing.getEndAt(),
                    "Overlaps an existing booking for " + existing.getClientName()));
        }
        return conflicts;
    }

    private boolean fitsWithinWorkSlots(List<TimeSlot> workSlots, ZonedDateTime start, ZonedDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false; // cross-midnight bookings are not supported — see class javadoc
        }
        int startMin = start.getHour() * 60 + start.getMinute();
        int endMin = end.getHour() * 60 + end.getMinute();
        for (TimeSlot slot : workSlots) {
            if (minutesOf(slot.getStartTime()) <= startMin && endMin <= minutesOf(slot.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean isClosedByHoliday(StoreSettings settings, String staffId, LocalDate date) {
        return settings.getHolidays().stream().anyMatch(h -> h.getDate().equals(date)
                && (h.isAppliesToAll() || (h.getAppliesToStaffIds() != null && h.getAppliesToStaffIds().contains(staffId)))
                && !h.isHasSurcharge());
    }

    // ── surcharges / deposits / fees ─────────────────────────────────────────

    private List<AppliedSurcharge> computeSurcharges(StoreSettings settings, String staffId, ZonedDateTime localStart, Money basePrice) {
        List<AppliedSurcharge> result = new ArrayList<>();
        for (Holiday holiday : settings.getHolidays()) {
            if (holiday.getDate().equals(localStart.toLocalDate()) && holiday.isHasSurcharge()
                    && (holiday.isAppliesToAll() || (holiday.getAppliesToStaffIds() != null && holiday.getAppliesToStaffIds().contains(staffId)))) {
                result.add(surchargeOf(holiday.getName(), holiday.getSurchargeType(), holiday.getSurchargeValue(), basePrice));
            }
        }
        SurchargeDay today = SurchargeDay.valueOf(localStart.getDayOfWeek().name());
        int minutesNow = localStart.getHour() * 60 + localStart.getMinute();
        for (SurchargeRule rule : settings.getSurchargeRules()) {
            if (!rule.isActive()) {
                continue;
            }
            if (rule.getDays() != null && !rule.getDays().isEmpty() && !rule.getDays().contains(today)) {
                continue;
            }
            if (rule.getTimeStart() != null && rule.getTimeEnd() != null) {
                int s = minutesOf(rule.getTimeStart());
                int e = minutesOf(rule.getTimeEnd());
                if (minutesNow < s || minutesNow >= e) {
                    continue;
                }
            }
            result.add(surchargeOf(rule.getName(), rule.getSurchargeType(), rule.getSurchargeValue(), basePrice));
        }
        return result;
    }

    private AppliedSurcharge surchargeOf(String label, SurchargeType type, Double value, Money basePrice) {
        Money amount = amountFor(type, value, basePrice);
        return AppliedSurcharge.builder().label(label).type(type).value(value).amount(amount).build();
    }

    private Money amountFor(SurchargeType type, Double value, Money basePrice) {
        if (type == SurchargeType.PERCENTAGE) {
            return Money.ofAud(Math.round(basePrice.amountCents() * ((value == null ? 0 : value) / 100.0)));
        }
        return Money.ofAud(Math.round((value == null ? 0 : value) * 100));
    }

    private Money computeDeposit(StoreSettings settings, Money total) {
        if (settings.getDepositType() == DepositType.PERCENTAGE) {
            return Money.ofAud(Math.round(total.amountCents() * ((settings.getDepositValue() == null ? 0 : settings.getDepositValue()) / 100.0)));
        }
        return Money.ofAud(Math.round((settings.getDepositValue() == null ? 0 : settings.getDepositValue()) * 100));
    }

    private Money feeAmount(au.com.getkempt.enums.store.FeeType type, Double value, Money basePrice) {
        if (type == au.com.getkempt.enums.store.FeeType.PERCENTAGE) {
            return Money.ofAud(Math.round(basePrice.amountCents() * ((value == null ? 0 : value) / 100.0)));
        }
        return Money.ofAud(Math.round((value == null ? 0 : value) * 100));
    }

    private void applyCancellationFee(Booking booking, StoreSettings settings) {
        long hoursUntil = java.time.temporal.ChronoUnit.HOURS.between(Instant.now(), booking.getStartAt());
        settings.getCancellationFeeRules().stream()
                .filter(r -> hoursUntil <= r.getWithinHours())
                .min((a, b) -> Integer.compare(a.getWithinHours(), b.getWithinHours()))
                .ifPresent(rule -> {
                    Money fee = amountFor(rule.getFeeType(), rule.getFeeValue(), booking.getPrice());
                    if (booking.getPaymentIntentId() != null) {
                        paymentGateway.capture(booking.getPaymentIntentId()); // stub: full capture; a real gateway would do a partial capture + refund remainder
                        booking.setPaymentCaptured(true);
                    } else {
                        PaymentGateway.IntentResult result = paymentGateway.createIntent(new PaymentGateway.CreateIntent(
                                fee, "cancellation_fee", false, Map.of("bookingId", booking.getId())));
                        booking.setPaymentIntentId(result.intentId());
                    }
                });
    }

    /**
     * Sends a client-facing lifecycle notification (confirmed/cancelled/rescheduled)
     * via the Outbox — see {@code OutboxService} javadoc for why this is enqueued
     * rather than sent inline. Honors the client's per-channel opt-in flags
     * ({@code Client.emailOptIn}/{@code smsOptIn}); a missing client record (e.g.
     * deleted after the booking was made) falls back to the same "opted in"
     * default those flags carry on a fresh {@code Client}, rather than silently
     * dropping the message.
     */
    private void notifyClientOfBookingEvent(Booking booking, String eventType, String subject, String smsBody, String emailHtmlBody) {
        Client client = clientRepository.findById(booking.getClientId()).orElse(null);
        boolean emailOptIn = client == null || client.isEmailOptIn();
        boolean smsOptIn = client == null || client.isSmsOptIn();
        String shopId = TenantContextHolder.requireShopId();
        if (emailOptIn && booking.getClientEmail() != null && !booking.getClientEmail().isBlank()) {
            outboxService.enqueueEmail(shopId, booking.getClientEmail(), subject, emailHtmlBody, eventType, booking.getId());
        }
        if (smsOptIn && booking.getClientPhone() != null && !booking.getClientPhone().isBlank()) {
            outboxService.enqueueSms(shopId, booking.getClientPhone(), smsBody, eventType, booking.getId());
        }
    }

    /** Keeps the {@link AppointmentRecord} read-model in sync — see its class javadoc. */
    private void updateAppointmentStatus(String bookingId, AppointmentStatus status, String note) {
        appointmentRecordRepository.findByBookingId(bookingId).ifPresent(record -> {
            record.setStatus(status);
            record.setStatusUpdatedAt(Instant.now());
            record.setStatusUpdatedByStaffId(TenantContextHolder.current().map(TenantContext::staffId).orElse(null));
            if (note != null) {
                record.setNote(note);
            }
            appointmentRecordRepository.save(record);
        });
    }

    private Money sumSurcharges(List<AppliedSurcharge> surcharges) {
        Money total = Money.zero();
        if (surcharges == null) {
            return total;
        }
        for (AppliedSurcharge s : surcharges) {
            total = total.plus(s.getAmount());
        }
        return total;
    }

    private PaymentIntentResponse toPaymentIntentResponse(PaymentGateway.IntentResult result, Money amount, String bookingId, String clientId) {
        return new PaymentIntentResponse(result.intentId(), bookingId, clientId, amount, "automatic", result.status(), result.clientSecret(), Instant.now());
    }

    // ── slot locks ──────────────────────────────────────────────────────────

    private void acquireSlotLocks(String bookingId, String staffId, Instant startAt, Instant endAt) {
        int bucketMinutes = Math.max(1, bookingProperties.getSlotLockBucketMinutes());
        Instant bucket = floorToBucket(startAt, bucketMinutes);
        while (bucket.isBefore(endAt)) {
            SlotLock lock = new SlotLock();
            lock.setStaffId(staffId);
            lock.setBucketStart(bucket);
            lock.setBookingId(bookingId);
            try {
                slotLockRepository.insert(lock);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // Another transaction already holds this bucket — surfaced as the
                // same 409 a conflicting overlap query would report, per
                // docs/05-booking-lifecycle.md's "Race condition handling".
                throw new ConflictException("The requested slot was just taken by another booking");
            }
            bucket = bucket.plusSeconds(bucketMinutes * 60L);
        }
    }

    private void reacquireSlotLocks(String bookingId, String staffId, Instant startAt, Instant endAt) {
        slotLockRepository.releaseAll(bookingId);
        acquireSlotLocks(bookingId, staffId, startAt, endAt);
    }

    private Instant floorToBucket(Instant instant, int bucketMinutes) {
        long epochMinutes = instant.getEpochSecond() / 60;
        long flooredMinutes = (epochMinutes / bucketMinutes) * bucketMinutes;
        return Instant.ofEpochSecond(flooredMinutes * 60);
    }

    // ── snapshot resolution ─────────────────────────────────────────────────

    private record Snapshot(String serviceId, String packageId, String name, int durationMinutes, Money price) {
    }

    private Snapshot resolveSnapshot(String serviceId, String packageId, StaffMember staff) {
        if (serviceId != null) {
            au.com.getkempt.entities.Service service = requireServiceForStaff(serviceId, staff.getId());
            return new Snapshot(service.getId(), null, service.getName(),
                    service.getDurationMinutes() + service.getBufferMinutes(), service.getPrice());
        }
        au.com.getkempt.entities.Package pkg = packageRepository.requireById(packageId);
        if (!nullSafe(pkg.getStaffIds()).contains(staff.getId())) {
            throw new UnprocessableEntityException("Staff member does not perform this package");
        }
        List<au.com.getkempt.entities.Service> services = serviceRepository.findByIds(pkg.getServiceIds());
        PackagePricingService.Pricing pricing = packagePricingService.compute(services, pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(), pkg.getDiscountType(), pkg.getDiscountValue());
        return new Snapshot(null, pkg.getId(), pkg.getName(), pricing.effectiveDurationMinutes(), pricing.discountedPrice());
    }

    /** Slot-search variant: no staff resolved yet, so staff-performs-service isn't checked here. */
    private Snapshot resolveSnapshotForStaffless(String serviceId, String packageId) {
        if (serviceId != null) {
            au.com.getkempt.entities.Service service = serviceRepository.requireById(serviceId);
            return new Snapshot(service.getId(), null, service.getName(),
                    service.getDurationMinutes() + service.getBufferMinutes(), service.getPrice());
        }
        au.com.getkempt.entities.Package pkg = packageRepository.requireById(packageId);
        List<au.com.getkempt.entities.Service> services = serviceRepository.findByIds(pkg.getServiceIds());
        PackagePricingService.Pricing pricing = packagePricingService.compute(services, pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(), pkg.getDiscountType(), pkg.getDiscountValue());
        return new Snapshot(null, pkg.getId(), pkg.getName(), pricing.effectiveDurationMinutes(), pricing.discountedPrice());
    }

    private au.com.getkempt.entities.Service requireServiceForStaff(String serviceId, String staffId) {
        au.com.getkempt.entities.Service service = serviceRepository.requireById(serviceId);
        if (!nullSafe(service.getStaffIds()).contains(staffId)) {
            throw new UnprocessableEntityException("Staff member does not perform this service");
        }
        return service;
    }

    private Client resolveClient(String clientId, InlineClientRequest inline) {
        if (clientId != null) {
            return clientRepository.requireById(clientId);
        }
        if (inline == null) {
            throw new UnprocessableEntityException("Either clientId or an inline client is required");
        }
        return clientRepository.findByPhone(inline.phone()).orElseGet(() -> {
            Client client = new Client();
            client.setFirstName(inline.firstName());
            client.setLastName(inline.lastName());
            client.setEmail(inline.email());
            client.setPhone(inline.phone());
            client.setJoinedAt(Instant.now());
            return clientRepository.insert(client);
        });
    }

    /** True if [startMin, endMin) (minutes-of-day) overlaps any of the day's break blocks. */
    private boolean overlapsBreak(List<TimeSlot> breaks, int startMin, int endMin) {
        if (breaks == null || breaks.isEmpty()) {
            return false;
        }
        for (TimeSlot b : breaks) {
            int breakStart = minutesOf(b.getStartTime());
            int breakEnd = minutesOf(b.getEndTime());
            if (startMin < breakEnd && endMin > breakStart) {
                return true;
            }
        }
        return false;
    }

    /**
     * Materialises a staff member's recurring/override BREAK blocks into absolute
     * instants across [from, to], so the calendar can render them alongside
     * bookings and holidays. Walks the range one local day at a time (breaks are
     * defined as minutes-of-day in the staff's timezone) and clips each block to
     * the requested window.
     */
    private List<CalendarResponse.BreakBlock> breaksInRange(String staffId, Instant from, Instant to) {
        List<CalendarResponse.BreakBlock> blocks = new ArrayList<>();
        ZoneId zone = ZoneId.of(availabilityService.timezoneFor(staffId));
        LocalDate day = from.atZone(zone).toLocalDate();
        LocalDate lastDay = to.atZone(zone).toLocalDate();
        while (!day.isAfter(lastDay)) {
            AvailabilityService.EffectiveDay effectiveDay = availabilityService.effectiveDay(staffId, day);
            if (effectiveDay.isWorking()) {
                for (TimeSlot b : effectiveDay.breakSlots()) {
                    Instant startAt = day.atStartOfDay(zone).plusMinutes(minutesOf(b.getStartTime())).toInstant();
                    Instant endAt = day.atStartOfDay(zone).plusMinutes(minutesOf(b.getEndTime())).toInstant();
                    if (endAt.isAfter(from) && startAt.isBefore(to)) {
                        blocks.add(new CalendarResponse.BreakBlock(staffId, startAt, endAt, b.getLabel()));
                    }
                }
            }
            day = day.plusDays(1);
        }
        return blocks;
    }

    private int minutesOf(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }

    private Set<BookingStatus> activeStatuses() {
        return BookingStatus.ACTIVE;
    }

    private StoreSettings requireSettings() {
        String shopId = TenantContextHolder.requireShopId();
        return storeSettingsRepository.findById(shopId).orElseThrow(() -> NotFoundException.of("StoreSettings", shopId));
    }

    private void requireAllowedInStoreMethod(au.com.getkempt.enums.store.InStoreMethod method) {
        paymentMethodPolicy.requireAllowed(requireSettings(), method);
    }

    // ── access control ──────────────────────────────────────────────────────

    private void requireCanSee(Booking booking) {
        TenantContext ctx = TenantContextHolder.require();
        if (ctx.hasPermission(Permission.BOOKINGS_READ_ALL) || ctx.isOwner()) {
            return;
        }
        if (!booking.getStaffId().equals(ctx.staffId())) {
            throw new ForbiddenException("You can only view your own bookings");
        }
    }

    private TenantContext requireCanCreateFor(String staffId) {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.BOOKINGS_WRITE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to create bookings");
        }
        if (!ctx.hasPermission(Permission.BOOKINGS_READ_ALL) && !ctx.isOwner() && !staffId.equals(ctx.staffId())) {
            throw new ForbiddenException("Staff members can only book for themselves");
        }
        return ctx;
    }

    private void requireCanWrite(Booking booking) {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.BOOKINGS_WRITE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to modify bookings");
        }
        if (!ctx.hasPermission(Permission.BOOKINGS_READ_ALL) && !ctx.isOwner() && !booking.getStaffId().equals(ctx.staffId())) {
            throw new ForbiddenException("Staff members can only modify their own bookings");
        }
    }

    private void requireCancelAccess() {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.BOOKINGS_CANCEL) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to cancel bookings");
        }
    }

    private void requireReadAllAccess() {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.BOOKINGS_READ_ALL) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to export bookings");
        }
    }
}
