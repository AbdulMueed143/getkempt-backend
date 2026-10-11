package au.com.getkempt.controllers.booking;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.booking.BookingSource;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.request.booking.BookingBulkRequest;
import au.com.getkempt.request.booking.BookingCreateRequest;
import au.com.getkempt.request.booking.BookingExportRequest;
import au.com.getkempt.request.booking.BookingNotesRequest;
import au.com.getkempt.request.booking.BookingUpdateRequest;
import au.com.getkempt.request.booking.CancelRequest;
import au.com.getkempt.request.booking.CompleteBookingRequest;
import au.com.getkempt.request.booking.ConflictCheckRequest;
import au.com.getkempt.request.booking.NoShowRequest;
import au.com.getkempt.request.booking.RescheduleRequest;
import au.com.getkempt.request.booking.SlotSearchRequest;
import au.com.getkempt.responses.booking.BookingBulkResponse;
import au.com.getkempt.responses.booking.BookingCreateResponse;
import au.com.getkempt.responses.booking.BookingExportResponse;
import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.responses.booking.CalendarResponse;
import au.com.getkempt.responses.booking.ConflictCheckResponse;
import au.com.getkempt.responses.booking.IcalFeedResponse;
import au.com.getkempt.responses.booking.SlotSearchResponse;
import au.com.getkempt.services.booking.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** {@code /api/v1/bookings} — see {@code paths/bookings.yaml}. */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public CursorPage<BookingResponse> listBookings(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) List<BookingStatus> status,
            @RequestParam(required = false) BookingSource source) {
        return bookingService.list(cursor, limit, sort, fromDate, toDate, staffId, clientId, serviceId, status, source);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingCreateResponse createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/calendar")
    public CalendarResponse getCalendar(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) List<String> staffIds,
            @RequestParam(required = false, defaultValue = "true") boolean includeBreaks) {
        return bookingService.getCalendar(from, to, staffIds, includeBreaks);
    }

    @PostMapping("/slots/search")
    public SlotSearchResponse searchSlots(@Valid @RequestBody SlotSearchRequest request) {
        return bookingService.searchSlots(request);
    }

    @PostMapping("/conflicts/check")
    public ConflictCheckResponse checkConflicts(@Valid @RequestBody ConflictCheckRequest request) {
        return bookingService.checkConflicts(request);
    }

    @PostMapping("/bulk")
    public BookingBulkResponse bulkBookings(@Valid @RequestBody BookingBulkRequest request) {
        return bookingService.bulk(request);
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BookingExportResponse exportBookings(@RequestBody(required = false) BookingExportRequest request) {
        String jobId = bookingService.export(
                request == null ? null : request.from(),
                request == null ? null : request.to(),
                request == null ? null : request.status());
        return new BookingExportResponse(jobId);
    }

    @GetMapping("/ical")
    public IcalFeedResponse getIcalFeed(@RequestParam(required = false) String staffId) {
        return new IcalFeedResponse(bookingService.getIcalFeedUrl(staffId));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable String bookingId) {
        return bookingService.get(bookingId);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponse updateBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody BookingUpdateRequest request) {
        return bookingService.update(bookingId, ifMatch, request);
    }

    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable String bookingId) {
        bookingService.delete(bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable String bookingId, @Valid @RequestBody CancelRequest request) {
        return bookingService.cancel(bookingId, request);
    }

    @PostMapping("/{bookingId}/reschedule")
    public BookingResponse rescheduleBooking(@PathVariable String bookingId, @Valid @RequestBody RescheduleRequest request) {
        return bookingService.reschedule(bookingId, request);
    }

    @PostMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(@PathVariable String bookingId) {
        return bookingService.confirm(bookingId);
    }

    @PostMapping("/{bookingId}/complete")
    public BookingResponse completeBooking(
            @PathVariable String bookingId,
            @RequestBody(required = false) CompleteBookingRequest request) {
        return bookingService.complete(bookingId, request);
    }

    @PostMapping("/{bookingId}/no-show")
    public BookingResponse markBookingNoShow(
            @PathVariable String bookingId,
            @RequestBody(required = false) NoShowRequest request) {
        return bookingService.noShow(bookingId, request);
    }

    @PutMapping("/{bookingId}/notes")
    public BookingResponse setBookingNotes(@PathVariable String bookingId, @Valid @RequestBody BookingNotesRequest request) {
        return bookingService.setNotes(bookingId, request);
    }
}
