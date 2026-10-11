package au.com.getkempt.controllers.booking;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.booking.AppointmentStatus;
import au.com.getkempt.request.booking.VisitBulkRequest;
import au.com.getkempt.request.booking.VisitMarkRequest;
import au.com.getkempt.responses.booking.AppointmentRecordResponse;
import au.com.getkempt.responses.booking.VisitBulkResponse;
import au.com.getkempt.responses.booking.VisitStatsResponse;
import au.com.getkempt.services.booking.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** {@code /api/v1/visits} — see {@code paths/visits.yaml}. */
@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping
    public CursorPage<AppointmentRecordResponse> listVisits(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) List<AppointmentStatus> status,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String clientId) {
        return visitService.list(cursor, limit, fromDate, toDate, status, staffId, clientId);
    }

    @GetMapping("/stats")
    public VisitStatsResponse getVisitStats() {
        return visitService.stats();
    }

    @PostMapping("/bulk")
    public VisitBulkResponse bulkVisits(@Valid @RequestBody VisitBulkRequest request) {
        return visitService.bulk(request);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentRecordResponse getVisit(@PathVariable String appointmentId) {
        return visitService.get(appointmentId);
    }

    @PostMapping("/{appointmentId}/mark-attended")
    public AppointmentRecordResponse markAttended(
            @PathVariable String appointmentId,
            @RequestBody(required = false) VisitMarkRequest request) {
        return visitService.markAttended(appointmentId, request);
    }

    @PostMapping("/{appointmentId}/mark-no-show")
    public AppointmentRecordResponse markNoShow(
            @PathVariable String appointmentId,
            @RequestBody(required = false) VisitMarkRequest request) {
        return visitService.markNoShow(appointmentId, request);
    }
}
