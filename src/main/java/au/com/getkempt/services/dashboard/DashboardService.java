package au.com.getkempt.services.dashboard;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.entities.booking.Booking;
import au.com.getkempt.entities.dashboard.DismissedAlert;
import au.com.getkempt.enums.booking.AppointmentStatus;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.mappers.BookingMapper;
import au.com.getkempt.repositories.booking.AppointmentRecordRepository;
import au.com.getkempt.repositories.booking.BookingRepository;
import au.com.getkempt.repositories.client.ClientRepository;
import au.com.getkempt.repositories.dashboard.DismissedAlertRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.responses.dashboard.DashboardAlertResponse;
import au.com.getkempt.responses.dashboard.DashboardChartPointResponse;
import au.com.getkempt.responses.dashboard.DashboardStatsResponse;
import au.com.getkempt.services.availability.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aggregated read views for the home page. Everything here is computed live
 * from Bookings/Clients/Availability at request time — the contract notes
 * these are "cacheable (short TTL)" on the CLIENT side, so no server-side
 * caching layer is added; a slow query here is a candidate for one later.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<String> CHART_RANGES = Set.of("last_7d", "last_30d", "last_90d", "ytd");

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRecordRepository appointmentRecordRepository;
    private final StaffRepository staffRepository;
    private final AvailabilityService availabilityService;
    private final DismissedAlertRepository dismissedAlertRepository;
    private final BookingMapper bookingMapper;

    private static final ZoneId UTC = ZoneId.of("UTC");

    public DashboardStatsResponse getStats() {
        LocalDate today = LocalDate.now(UTC);
        LocalDate yesterday = today.minusDays(1);

        long bookingsToday = countBookingsOnDay(today);
        long bookingsYesterday = countBookingsOnDay(yesterday);
        Double bookingsTrend = trendPercent(bookingsToday, bookingsYesterday);

        long revenueTodayCents = sumRevenueOnDay(today);
        long revenueYesterdayCents = sumRevenueOnDay(yesterday);
        Double revenueTrend = trendPercent(revenueTodayCents, revenueYesterdayCents);

        long newClientsThisWeek = clientRepository.count(Criteria.where("joinedAt").gte(Instant.now().minusSeconds(7L * 86400)));
        long pendingReviewCount = appointmentRecordRepository.count(Criteria.where("status").is(AppointmentStatus.PENDING));

        double occupancyRate = computeOccupancyRate(today);

        return new DashboardStatsResponse(
                (int) bookingsToday, bookingsTrend, Money.ofAud(Math.max(0, revenueTodayCents)), revenueTrend,
                (int) newClientsThisWeek, occupancyRate, (int) pendingReviewCount,
                null); // SMS credits: Billing domain not yet implemented
    }

    public List<DashboardChartPointResponse> getChart(String range, List<String> metrics) {
        if (range == null || !CHART_RANGES.contains(range)) {
            throw new BadRequestException("range must be one of " + CHART_RANGES);
        }
        LocalDate today = LocalDate.now(UTC);
        LocalDate from = switch (range) {
            case "last_7d" -> today.minusDays(6);
            case "last_30d" -> today.minusDays(29);
            case "last_90d" -> today.minusDays(89);
            default -> today.withDayOfYear(1); // ytd
        };

        // Day-by-day queries rather than a single aggregation pipeline — simple
        // and correct, at the cost of O(days) round-trips; fine at dashboard
        // scale (<= ~366 rows/year), but a $group aggregation would be the move
        // if this ever needs to serve much longer ranges.
        List<DashboardChartPointResponse> points = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(UTC).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(UTC).toInstant();
            long bookings = bookingRepository.count(Criteria.where("startAt").gte(dayStart).lt(dayEnd)
                    .and("status").ne(BookingStatus.CANCELLED));
            long revenueCents = sumRevenueBetween(dayStart, dayEnd);
            Integer noShows = null;
            if (metrics == null || metrics.isEmpty() || metrics.contains("no_shows")) {
                noShows = (int) appointmentRecordRepository.count(Criteria.where("scheduledAt").gte(dayStart).lt(dayEnd)
                        .and("status").is(AppointmentStatus.NO_SHOW));
            }
            points.add(new DashboardChartPointResponse(date, (int) bookings, revenueCents, null, noShows));
        }
        return points;
    }

    public List<BookingResponse> getUpcomingBookings(Integer limit) {
        int resolvedLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        Criteria filter = Criteria.where("startAt").gte(Instant.now()).and("status").in(BookingStatus.ACTIVE);
        List<Booking> bookings = bookingRepository.findMatching(filter, Sort.by(Sort.Direction.ASC, "startAt"), resolvedLimit);
        return bookings.stream().map(bookingMapper::toResponse).toList();
    }

    // ── alerts (computed, not persisted — see DismissedAlert javadoc) ──────

    public List<DashboardAlertResponse> listAlerts(boolean unreadOnly) {
        String shopId = TenantContextHolder.requireShopId();
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        long pendingReview = appointmentRecordRepository.count(Criteria.where("status").is(AppointmentStatus.PENDING));
        if (pendingReview > 0) {
            String key = "pending_review:" + LocalDate.now(UTC);
            boolean dismissed = dismissedAlertRepository.existsByShopIdAndAlertKey(shopId, key);
            if (!dismissed || !unreadOnly) {
                alerts.add(new DashboardAlertResponse(key, "warning", "Appointments awaiting review",
                        pendingReview + " appointment(s) need to be marked attended or no-show", Instant.now(),
                        "Review visits", "/visits", dismissed));
            }
        }

        // More alert types (low SMS credits, pending payouts, failed campaign
        // sends) will slot in here once Billing/Payments/Campaigns exist.
        return alerts;
    }

    public void dismissAlert(String alertId) {
        String shopId = TenantContextHolder.requireShopId();
        if (!dismissedAlertRepository.existsByShopIdAndAlertKey(shopId, alertId)) {
            dismissedAlertRepository.save(DismissedAlert.builder()
                    .shopId(shopId)
                    .alertKey(alertId)
                    .dismissedAt(Instant.now())
                    .build());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long countBookingsOnDay(LocalDate date) {
        Instant dayStart = date.atStartOfDay(UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(UTC).toInstant();
        return bookingRepository.count(Criteria.where("startAt").gte(dayStart).lt(dayEnd).and("status").ne(BookingStatus.CANCELLED));
    }

    private long sumRevenueOnDay(LocalDate date) {
        Instant dayStart = date.atStartOfDay(UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(UTC).toInstant();
        return sumRevenueBetween(dayStart, dayEnd);
    }

    private long sumRevenueBetween(Instant from, Instant to) {
        Criteria filter = Criteria.where("startAt").gte(from).lt(to).and("status").is(BookingStatus.COMPLETED);
        return bookingRepository.findMatching(filter).stream().mapToLong(b -> b.getPrice().amountCents()).sum();
    }

    private Double trendPercent(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : null; // undefined trend with no baseline
        }
        return ((current - previous) * 100.0) / previous;
    }

    private double computeOccupancyRate(LocalDate today) {
        List<StaffMember> staff = staffRepository.list(new Criteria(),
                        au.com.getkempt.common.query.SortRequest.asc("createdAt"),
                        au.com.getkempt.common.pagination.CursorPageRequest.of(null, au.com.getkempt.common.pagination.CursorPageRequest.MAX_LIMIT))
                .data();
        int totalWorkingMinutes = 0;
        for (StaffMember member : staff) {
            AvailabilityService.EffectiveDay effectiveDay = availabilityService.effectiveDay(member.getId(), today);
            if (effectiveDay.isWorking()) {
                totalWorkingMinutes += effectiveDay.workSlots().stream()
                        .mapToInt(s -> minutesOf(s.getEndTime()) - minutesOf(s.getStartTime()))
                        .sum();
            }
        }
        if (totalWorkingMinutes == 0) {
            return 0.0;
        }
        Instant dayStart = today.atStartOfDay(UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(UTC).toInstant();
        Set<BookingStatus> activeOrCompleted = new java.util.HashSet<>(BookingStatus.ACTIVE);
        activeOrCompleted.add(BookingStatus.COMPLETED);
        Criteria filter = Criteria.where("startAt").gte(dayStart).lt(dayEnd).and("status").in(activeOrCompleted);
        int bookedMinutes = bookingRepository.findMatching(filter).stream().mapToInt(Booking::getDurationMinutes).sum();
        return Math.min(100.0, (bookedMinutes * 100.0) / totalWorkingMinutes);
    }

    private int minutesOf(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
