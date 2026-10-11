package au.com.getkempt.services.availability;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.availability.AvailabilityOverride;
import au.com.getkempt.entities.availability.DaySchedule;
import au.com.getkempt.entities.availability.TimeSlot;
import au.com.getkempt.entities.availability.WeeklySchedule;
import au.com.getkempt.enums.availability.SlotType;
import au.com.getkempt.enums.availability.TimelineStatus;
import au.com.getkempt.mappers.AvailabilityMapper;
import au.com.getkempt.repositories.availability.AvailabilityOverrideRepository;
import au.com.getkempt.repositories.availability.WeeklyScheduleRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.repositories.store.StoreProfileRepository;
import au.com.getkempt.request.availability.BreakCreateRequest;
import au.com.getkempt.request.availability.BulkAvailabilityUpdateRequest;
import au.com.getkempt.request.availability.CopyScheduleRequest;
import au.com.getkempt.request.availability.DayScheduleRequest;
import au.com.getkempt.request.availability.OverrideCreateRequest;
import au.com.getkempt.request.availability.OverrideUpdateRequest;
import au.com.getkempt.request.availability.TimeSlotRequest;
import au.com.getkempt.request.availability.WeeklyScheduleWriteRequest;
import au.com.getkempt.responses.availability.AvailabilityOverrideResponse;
import au.com.getkempt.responses.availability.BulkAvailabilityUpdateResponse;
import au.com.getkempt.responses.availability.CopyScheduleResponse;
import au.com.getkempt.responses.availability.TimeSlotResponse;
import au.com.getkempt.responses.availability.TimelineEntryResponse;
import au.com.getkempt.responses.availability.WeeklyScheduleResponse;
import au.com.getkempt.responses.store.HolidayResponse;
import au.com.getkempt.services.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-staff weekly schedules + date overrides + recurring breaks. This is the
 * source of truth the Bookings domain queries for slot validation — see
 * {@link #effectiveDay} — so its rules ("override beats weekly schedule",
 * "0 = Sunday") are centralised here rather than duplicated in {@code BookingService}.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final AvailabilityOverrideRepository overrideRepository;
    private final StaffRepository staffRepository;
    private final StoreProfileRepository storeProfileRepository;
    private final StoreService storeService;
    private final AvailabilityMapper mapper;

    // ── weekly schedule ─────────────────────────────────────────────────────

    public WeeklyScheduleResponse getWeeklySchedule(String staffId) {
        staffRepository.requireById(staffId);
        // A pure GET must not have the side effect of writing a document, so unlike
        // the mutation paths below (which call getOrCreate to guarantee something
        // exists to attach the change to), an unconfigured staff member gets a
        // transient, never-persisted "all days off" default here.
        WeeklySchedule schedule = weeklyScheduleRepository.findById(staffId).orElseGet(() -> {
            WeeklySchedule fresh = new WeeklySchedule();
            fresh.setId(staffId);
            fresh.setStaffId(staffId);
            fresh.setTimezone(defaultTimezone());
            fresh.setDays(WeeklySchedule.defaultDays());
            fresh.setUpdatedAt(java.time.Instant.now()); // schema requires updatedAt even before the first real save
            return fresh;
        });
        return mapper.toResponse(schedule);
    }

    public WeeklyScheduleResponse setWeeklySchedule(String staffId, String ifMatch, WeeklyScheduleWriteRequest request) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        validateDays(request.days());

        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        IfMatchSupport.check(ifMatch, schedule.getVersion());

        schedule.setTimezone(request.timezone());
        schedule.setDays(toDayEntities(request.days()));
        weeklyScheduleRepository.save(schedule);
        return mapper.toResponse(schedule);
    }

    // ── overrides ───────────────────────────────────────────────────────────

    public List<AvailabilityOverrideResponse> listOverrides(String staffId, LocalDate from, LocalDate to) {
        staffRepository.requireById(staffId);
        List<AvailabilityOverride> overrides = (from != null && to != null)
                ? overrideRepository.findByStaffIdAndDateRange(staffId, from, to)
                : overrideRepository.findByStaffId(staffId);
        return overrides.stream()
                .sorted(Comparator.comparing(AvailabilityOverride::getDate))
                .map(mapper::toResponse)
                .toList();
    }

    public AvailabilityOverrideResponse createOverride(String staffId, OverrideCreateRequest request) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        overrideRepository.findByStaffIdAndDate(staffId, request.date()).ifPresent(existing -> {
            throw new ConflictException(
                    "An override already exists for %s — use PATCH /overrides/%s".formatted(request.date(), existing.getId()));
        });

        List<TimeSlot> slots = toSlotEntities(request.slots());
        if (Boolean.TRUE.equals(request.isWorking())) {
            validateNoOverlap(slots, request.date().toString());
        }

        AvailabilityOverride override = new AvailabilityOverride();
        override.setStaffId(staffId);
        override.setDate(request.date());
        override.setWorking(request.isWorking());
        override.setSlots(slots);
        override.setNote(request.note());
        overrideRepository.insert(override);
        return mapper.toResponse(override);
    }

    public AvailabilityOverrideResponse updateOverride(String staffId, String overrideId, OverrideUpdateRequest request) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        AvailabilityOverride override = requireOverrideForStaff(staffId, overrideId);

        if (request.isWorking() != null) {
            override.setWorking(request.isWorking());
        }
        if (request.slots() != null) {
            override.setSlots(toSlotEntities(request.slots()));
        }
        if (request.note() != null) {
            override.setNote(request.note());
        }
        if (override.isWorking()) {
            validateNoOverlap(override.getSlots(), override.getDate().toString());
        }
        overrideRepository.save(override);
        return mapper.toResponse(override);
    }

    public void deleteOverride(String staffId, String overrideId) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        overrideRepository.softDelete(requireOverrideForStaff(staffId, overrideId));
    }

    // ── copy ────────────────────────────────────────────────────────────────

    public CopyScheduleResponse copySchedule(String staffId, CopyScheduleRequest request) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        WeeklySchedule source = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        boolean overwriteOverrides = Boolean.TRUE.equals(request.overwriteOverrides());
        List<AvailabilityOverride> sourceOverrides = overwriteOverrides ? overrideRepository.findByStaffId(staffId) : List.of();

        for (String targetStaffId : request.targetStaffIds()) {
            if (targetStaffId.equals(staffId)) {
                continue; // copying onto oneself is a no-op, not an error
            }
            staffRepository.requireById(targetStaffId);

            WeeklySchedule target = weeklyScheduleRepository.getOrCreate(targetStaffId, defaultTimezone());
            target.setTimezone(source.getTimezone());
            target.setDays(cloneDaysWithFreshIds(source.getDays()));
            weeklyScheduleRepository.save(target);

            if (overwriteOverrides) {
                for (AvailabilityOverride sourceOverride : sourceOverrides) {
                    AvailabilityOverride targetOverride = overrideRepository
                            .findByStaffIdAndDate(targetStaffId, sourceOverride.getDate())
                            .orElseGet(AvailabilityOverride::new);
                    targetOverride.setStaffId(targetStaffId);
                    targetOverride.setDate(sourceOverride.getDate());
                    targetOverride.setWorking(sourceOverride.isWorking());
                    targetOverride.setSlots(cloneSlotsWithFreshIds(sourceOverride.getSlots()));
                    targetOverride.setNote(sourceOverride.getNote());
                    if (targetOverride.getId() == null) {
                        overrideRepository.insert(targetOverride);
                    } else {
                        overrideRepository.save(targetOverride);
                    }
                }
            }
        }
        return new CopyScheduleResponse(request.targetStaffIds());
    }

    // ── timeline ────────────────────────────────────────────────────────────

    public List<TimelineEntryResponse> getTimeline(String staffId, LocalDate from, LocalDate to) {
        staffRepository.requireById(staffId);
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 400) {
            throw new BadRequestException("Timeline range must not exceed ~400 days");
        }

        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        Map<LocalDate, AvailabilityOverride> overridesByDate = new HashMap<>();
        for (AvailabilityOverride override : overrideRepository.findByStaffIdAndDateRange(staffId, from, to)) {
            overridesByDate.put(override.getDate(), override);
        }

        List<TimelineEntryResponse> entries = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            AvailabilityOverride override = overridesByDate.get(date);
            if (override != null) {
                int minutes = override.isWorking() ? workMinutes(override.getSlots()) : 0;
                entries.add(new TimelineEntryResponse(date, TimelineStatus.OVERRIDE, minutes));
                continue;
            }
            DaySchedule day = dayFor(schedule, date);
            if (!day.isWorking()) {
                entries.add(new TimelineEntryResponse(date, TimelineStatus.OFF, 0));
                continue;
            }
            int minutes = workMinutes(day.getSlots());
            entries.add(new TimelineEntryResponse(date, minutes > 0 ? TimelineStatus.WORKING : TimelineStatus.PARTIAL, minutes));
        }
        return entries;
    }

    // ── breaks ──────────────────────────────────────────────────────────────

    public List<TimeSlotResponse> listBreaks(String staffId) {
        staffRepository.requireById(staffId);
        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        Map<String, TimeSlot> byId = new java.util.LinkedHashMap<>();
        for (DaySchedule day : schedule.getDays()) {
            for (TimeSlot slot : day.getSlots()) {
                if (slot.getType() == SlotType.BREAK) {
                    byId.putIfAbsent(slot.getId(), slot);
                }
            }
        }
        return byId.values().stream().map(mapper::toResponse).toList();
    }

    public TimeSlotResponse createBreak(String staffId, BreakCreateRequest request) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        if (!(minutesOf(request.startTime()) < minutesOf(request.endTime()))) {
            throw new UnprocessableEntityException("Break endTime must be after startTime");
        }

        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        TimeSlot breakSlot = TimeSlot.builder()
                .id(UUID.randomUUID().toString())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .type(SlotType.BREAK)
                .label(request.label())
                .build();

        boolean explicitDays = request.days() != null && !request.days().isEmpty();
        for (DaySchedule day : schedule.getDays()) {
            boolean applies = explicitDays ? request.days().contains(day.getDayOfWeek()) : day.isWorking();
            // A break only makes sense on a day the staff member is actually working;
            // silently skip closed days rather than erroring on a bulk "all days" add.
            if (applies && day.isWorking()) {
                day.getSlots().add(breakSlot);
                validateNoOverlap(day.getSlots(), "day " + day.getDayOfWeek());
            }
        }
        weeklyScheduleRepository.save(schedule);
        return mapper.toResponse(breakSlot);
    }

    public void deleteBreak(String staffId, String breakId) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        boolean removedAny = false;
        for (DaySchedule day : schedule.getDays()) {
            removedAny |= day.getSlots().removeIf(slot -> slot.getType() == SlotType.BREAK && slot.getId().equals(breakId));
        }
        if (!removedAny) {
            throw NotFoundException.of("Break", breakId);
        }
        weeklyScheduleRepository.save(schedule);
    }

    // ── bulk update ─────────────────────────────────────────────────────────

    /**
     * Owner-only shop-wide hours wizard. Wrapped in a Mongo multi-document
     * transaction (via {@code MongoTransactionManager}, already configured in
     * {@code MongoTransactionConfig}) because the contract explicitly promises
     * "update many staff schedules in one transaction" — a partial failure (e.g.
     * staff #7 of 12 has an invalid day) must roll back staff #1-6's writes too,
     * rather than leaving the shop's hours half-migrated. Requires Mongo running
     * as a replica set; see that class's javadoc.
     */
    @Transactional("mongoTransactionManager")
    public BulkAvailabilityUpdateResponse bulkUpdateAvailability(BulkAvailabilityUpdateRequest request) {
        requireOwnerOnly();
        List<String> updated = new ArrayList<>();
        for (BulkAvailabilityUpdateRequest.StaffDaysUpdate update : request.updates()) {
            staffRepository.requireById(update.staffId());
            validateDays(update.days());
            WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(update.staffId(), defaultTimezone());
            schedule.setDays(toDayEntities(update.days()));
            weeklyScheduleRepository.save(schedule);
            updated.add(update.staffId());
        }
        return new BulkAvailabilityUpdateResponse(updated);
    }

    // ── holidays alias ──────────────────────────────────────────────────────

    /** Alias of {@code GET /store/holidays}, per {@code paths/availability.yaml#/holidays}. */
    public List<HolidayResponse> getHolidays() {
        return storeService.listHolidays();
    }

    // ── shared slot-validation logic (reused by the Bookings domain) ───────

    /**
     * The effective working state for one staff member on one calendar date.
     * {@code breakSlots} are the (non-working) BREAK blocks carved out of the day —
     * the Bookings domain subtracts these from bookable time in slot-search and
     * conflict-detection so appointments can't be offered or accepted over a break.
     */
    public record EffectiveDay(boolean isWorking, List<TimeSlot> workSlots, List<TimeSlot> breakSlots, boolean isOverride) {
    }

    /**
     * Resolves what a staff member's hours actually are on {@code date}: an
     * {@link AvailabilityOverride}, if one exists for that date, otherwise the
     * recurring {@link WeeklySchedule} entry for that weekday. This is the single
     * place slot validation should ask "is this staff working, and when" — both
     * today's timeline endpoint and the future Bookings slot-search/conflict-check
     * logic call through here so the two can never disagree.
     */
    public EffectiveDay effectiveDay(String staffId, LocalDate date) {
        Optional<AvailabilityOverride> override = overrideRepository.findByStaffIdAndDate(staffId, date);
        if (override.isPresent()) {
            AvailabilityOverride o = override.get();
            return new EffectiveDay(o.isWorking(), workSlotsOnly(o.getSlots()), breakSlotsOnly(o.getSlots()), true);
        }
        WeeklySchedule schedule = weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone());
        DaySchedule day = dayFor(schedule, date);
        return new EffectiveDay(day.isWorking(), workSlotsOnly(day.getSlots()), breakSlotsOnly(day.getSlots()), false);
    }

    public String timezoneFor(String staffId) {
        return weeklyScheduleRepository.getOrCreate(staffId, defaultTimezone()).getTimezone();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private DaySchedule dayFor(WeeklySchedule schedule, LocalDate date) {
        int jsDayOfWeek = date.getDayOfWeek().getValue() % 7; // java MONDAY=1..SUNDAY=7 -> contract's 0=Sunday..6=Saturday
        return schedule.getDays().stream()
                .filter(d -> d.getDayOfWeek() == jsDayOfWeek)
                .findFirst()
                .orElseGet(() -> DaySchedule.builder().dayOfWeek(jsDayOfWeek).isWorking(false).slots(new ArrayList<>()).build());
    }

    private List<TimeSlot> workSlotsOnly(List<TimeSlot> slots) {
        return slots.stream().filter(s -> s.getType() == SlotType.WORK).toList();
    }

    private List<TimeSlot> breakSlotsOnly(List<TimeSlot> slots) {
        return slots.stream().filter(s -> s.getType() == SlotType.BREAK).toList();
    }

    private int workMinutes(List<TimeSlot> slots) {
        return slots.stream()
                .filter(s -> s.getType() == SlotType.WORK)
                .mapToInt(s -> minutesOf(s.getEndTime()) - minutesOf(s.getStartTime()))
                .sum();
    }

    private int minutesOf(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void validateDays(List<DayScheduleRequest> days) {
        if (days.size() != 7) {
            throw new UnprocessableEntityException("A week must have exactly 7 days");
        }
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (DayScheduleRequest day : days) {
            if (day.dayOfWeek() < 0 || day.dayOfWeek() > 6 || !seen.add(day.dayOfWeek())) {
                throw new UnprocessableEntityException("days must cover each of 0-6 exactly once");
            }
            if (day.isWorking() && day.slots() != null) {
                validateNoOverlap(toSlotEntities(day.slots()), "dayOfWeek " + day.dayOfWeek());
            }
        }
    }

    /**
     * 422s on overlapping slots or end &lt;= start, per {@code paths/availability.yaml}.
     * Overlap is checked <em>within each slot type</em> only: a BREAK is expected to
     * sit inside a WORK window (e.g. a lunch break during 09:00-17:00), so a
     * cross-type work/break overlap is legal — only work-vs-work and break-vs-break
     * overlaps are rejected.
     */
    private void validateNoOverlap(List<TimeSlot> slots, String context) {
        for (SlotType type : SlotType.values()) {
            List<TimeSlot> sorted = slots.stream()
                    .filter(s -> s.getType() == type)
                    .sorted(Comparator.comparingInt(s -> minutesOf(s.getStartTime())))
                    .toList();
            int previousEnd = -1;
            for (TimeSlot slot : sorted) {
                int start = minutesOf(slot.getStartTime());
                int end = minutesOf(slot.getEndTime());
                if (start >= 24 * 60 || end > 24 * 60) {
                    throw new UnprocessableEntityException("Time slots must fall within a 24h day (" + context + ")");
                }
                if (end <= start) {
                    throw new UnprocessableEntityException("Slot endTime must be after startTime (" + context + ")");
                }
                if (start < previousEnd) {
                    throw new UnprocessableEntityException("Time slots overlap (" + context + ")");
                }
                previousEnd = end;
            }
        }
    }

    private List<DaySchedule> toDayEntities(List<DayScheduleRequest> days) {
        return days.stream()
                .map(d -> DaySchedule.builder()
                        .dayOfWeek(d.dayOfWeek())
                        .isWorking(d.isWorking())
                        .slots(toSlotEntities(d.slots()))
                        .build())
                .toList();
    }

    private List<TimeSlot> toSlotEntities(List<TimeSlotRequest> slots) {
        if (slots == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(slots.stream()
                .map(s -> TimeSlot.builder()
                        .id(s.id() != null ? s.id() : UUID.randomUUID().toString())
                        .startTime(s.startTime())
                        .endTime(s.endTime())
                        .type(s.type())
                        .label(s.label())
                        .build())
                .toList());
    }

    private List<DaySchedule> cloneDaysWithFreshIds(List<DaySchedule> days) {
        return days.stream()
                .map(d -> DaySchedule.builder()
                        .dayOfWeek(d.getDayOfWeek())
                        .isWorking(d.isWorking())
                        .slots(cloneSlotsWithFreshIds(d.getSlots()))
                        .build())
                .toList();
    }

    private List<TimeSlot> cloneSlotsWithFreshIds(List<TimeSlot> slots) {
        return new ArrayList<>(slots.stream()
                .map(s -> TimeSlot.builder()
                        .id(UUID.randomUUID().toString())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .type(s.getType())
                        .label(s.getLabel())
                        .build())
                .toList());
    }

    private AvailabilityOverride requireOverrideForStaff(String staffId, String overrideId) {
        AvailabilityOverride override = overrideRepository.requireById(overrideId);
        if (!override.getStaffId().equals(staffId)) {
            throw NotFoundException.of("AvailabilityOverride", overrideId);
        }
        return override;
    }

    private String defaultTimezone() {
        return storeProfileRepository.findById(TenantContextHolder.requireShopId())
                .map(profile -> profile.getTimezone())
                .orElse("Australia/Melbourne");
    }

    private void requireManageAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.STAFF_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage availability");
        }
    }

    private void requireOwnerOnly() {
        if (!TenantContextHolder.require().isOwner()) {
            throw new ForbiddenException("Only the shop owner can run a bulk availability update");
        }
    }
}
