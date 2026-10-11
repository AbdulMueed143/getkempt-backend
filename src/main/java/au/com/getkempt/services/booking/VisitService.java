package au.com.getkempt.services.booking;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.entities.booking.AppointmentRecord;
import au.com.getkempt.enums.booking.AppointmentStatus;
import au.com.getkempt.enums.booking.CancellationReason;
import au.com.getkempt.mappers.BookingMapper;
import au.com.getkempt.repositories.booking.AppointmentRecordRepository;
import au.com.getkempt.request.booking.CancelRequest;
import au.com.getkempt.request.booking.CompleteBookingRequest;
import au.com.getkempt.request.booking.NoShowRequest;
import au.com.getkempt.request.booking.VisitBulkRequest;
import au.com.getkempt.request.booking.VisitMarkRequest;
import au.com.getkempt.responses.booking.AppointmentRecordResponse;
import au.com.getkempt.responses.booking.VisitBulkResponse;
import au.com.getkempt.responses.booking.VisitStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Post-appointment review — a thin layer over {@link AppointmentRecord} (the
 * materialised read-model) that delegates every actual state change to
 * {@link BookingService}'s own complete/no-show/cancel methods, so Visits and
 * Bookings can never disagree about who's allowed to do what.
 */
@Service
@RequiredArgsConstructor
public class VisitService {

    private static final Set<String> SORTABLE = Set.of("scheduledAt", "createdAt");

    private final AppointmentRecordRepository appointmentRecordRepository;
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    public CursorPage<AppointmentRecordResponse> list(String cursor, Integer limit,
                                                       java.time.LocalDate fromDate, java.time.LocalDate toDate,
                                                       List<AppointmentStatus> status, String staffId, String clientId) {
        List<Criteria> ands = new ArrayList<>();
        if (fromDate != null) {
            ands.add(Criteria.where("scheduledAt").gte(fromDate.atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        if (toDate != null) {
            ands.add(Criteria.where("scheduledAt").lt(toDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()));
        }
        if (status != null && !status.isEmpty()) {
            ands.add(Criteria.where("status").in(status));
        }
        if (staffId != null) {
            ands.add(Criteria.where("staffId").is(staffId));
        }
        if (clientId != null) {
            ands.add(Criteria.where("clientId").is(clientId));
        }
        Criteria filter = ands.isEmpty() ? new Criteria() : new Criteria().andOperator(ands.toArray(new Criteria[0]));
        SortRequest sort = SortRequest.parse(null, SORTABLE, SortRequest.desc("scheduledAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return appointmentRecordRepository.list(filter, sort, page).map(bookingMapper::toResponse);
    }

    public AppointmentRecordResponse get(String appointmentId) {
        return bookingMapper.toResponse(appointmentRecordRepository.requireById(appointmentId));
    }

    public AppointmentRecordResponse markAttended(String appointmentId, VisitMarkRequest request) {
        AppointmentRecord record = appointmentRecordRepository.requireById(appointmentId);
        bookingService.complete(record.getBookingId(), new CompleteBookingRequest(null, null, null));
        if (request != null && request.note() != null) {
            record.setNote(request.note());
            appointmentRecordRepository.save(record);
        }
        return bookingMapper.toResponse(appointmentRecordRepository.requireById(appointmentId));
    }

    public AppointmentRecordResponse markNoShow(String appointmentId, VisitMarkRequest request) {
        AppointmentRecord record = appointmentRecordRepository.requireById(appointmentId);
        bookingService.noShow(record.getBookingId(), new NoShowRequest(request != null ? request.note() : null, null));
        return bookingMapper.toResponse(appointmentRecordRepository.requireById(appointmentId));
    }

    public VisitBulkResponse bulk(VisitBulkRequest request) {
        List<String> succeeded = new ArrayList<>();
        List<VisitBulkResponse.Failure> failed = new ArrayList<>();
        for (String appointmentId : request.appointmentIds()) {
            try {
                AppointmentRecord record = appointmentRecordRepository.requireById(appointmentId);
                switch (request.action()) {
                    case "attended" -> bookingService.complete(record.getBookingId(), new CompleteBookingRequest(null, null, null));
                    case "no_show" -> bookingService.noShow(record.getBookingId(), new NoShowRequest(request.note(), null));
                    case "cancelled" -> bookingService.cancel(record.getBookingId(),
                            new CancelRequest(CancellationReason.OTHER, request.note(), false, false));
                    default -> throw new BadRequestException("Unknown visit bulk action: " + request.action());
                }
                succeeded.add(appointmentId);
            } catch (RuntimeException ex) {
                failed.add(new VisitBulkResponse.Failure(appointmentId, ex.getMessage()));
            }
        }
        return new VisitBulkResponse(succeeded, failed);
    }

    public VisitStatsResponse stats() {
        Instant now = Instant.now();
        ZonedDateTime todayStart = now.atZone(ZoneId.of("UTC")).toLocalDate().atStartOfDay(ZoneId.of("UTC"));

        long pendingReviewCount = appointmentRecordRepository.count(Criteria.where("status").is(AppointmentStatus.PENDING));
        long attendedTodayCount = appointmentRecordRepository.count(new Criteria().andOperator(
                Criteria.where("status").is(AppointmentStatus.COMPLETED),
                Criteria.where("statusUpdatedAt").gte(todayStart.toInstant())));
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        long noShowsLast7Days = appointmentRecordRepository.count(new Criteria().andOperator(
                Criteria.where("status").is(AppointmentStatus.NO_SHOW),
                Criteria.where("statusUpdatedAt").gte(sevenDaysAgo)));

        // Attendance rate isn't pinned to a specific window by the contract; a
        // trailing 30 days is used here as a representative, non-noisy sample.
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        long completed30d = appointmentRecordRepository.count(new Criteria().andOperator(
                Criteria.where("status").is(AppointmentStatus.COMPLETED),
                Criteria.where("statusUpdatedAt").gte(thirtyDaysAgo)));
        long noShow30d = appointmentRecordRepository.count(new Criteria().andOperator(
                Criteria.where("status").is(AppointmentStatus.NO_SHOW),
                Criteria.where("statusUpdatedAt").gte(thirtyDaysAgo)));
        long reviewed30d = completed30d + noShow30d;
        double attendanceRatePercent = reviewed30d == 0 ? 100.0 : (completed30d * 100.0) / reviewed30d;

        return new VisitStatsResponse(pendingReviewCount, attendedTodayCount, noShowsLast7Days, attendanceRatePercent);
    }
}
