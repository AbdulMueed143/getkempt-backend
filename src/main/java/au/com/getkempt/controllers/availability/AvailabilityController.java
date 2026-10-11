package au.com.getkempt.controllers.availability;

import au.com.getkempt.request.availability.BreakCreateRequest;
import au.com.getkempt.request.availability.BulkAvailabilityUpdateRequest;
import au.com.getkempt.request.availability.CopyScheduleRequest;
import au.com.getkempt.request.availability.OverrideCreateRequest;
import au.com.getkempt.request.availability.OverrideUpdateRequest;
import au.com.getkempt.request.availability.WeeklyScheduleWriteRequest;
import au.com.getkempt.responses.availability.AvailabilityOverrideResponse;
import au.com.getkempt.responses.availability.BulkAvailabilityUpdateResponse;
import au.com.getkempt.responses.availability.CopyScheduleResponse;
import au.com.getkempt.responses.availability.TimeSlotResponse;
import au.com.getkempt.responses.availability.TimelineEntryResponse;
import au.com.getkempt.responses.availability.WeeklyScheduleResponse;
import au.com.getkempt.responses.store.HolidayResponse;
import au.com.getkempt.services.availability.AvailabilityService;
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

import java.time.LocalDate;
import java.util.List;

/** {@code /api/v1/availability} — see {@code paths/availability.yaml}. */
@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{staffId}/weekly")
    public WeeklyScheduleResponse getWeeklySchedule(@PathVariable String staffId) {
        return availabilityService.getWeeklySchedule(staffId);
    }

    @PutMapping("/{staffId}/weekly")
    public WeeklyScheduleResponse setWeeklySchedule(
            @PathVariable String staffId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody WeeklyScheduleWriteRequest request) {
        return availabilityService.setWeeklySchedule(staffId, ifMatch, request);
    }

    @GetMapping("/{staffId}/overrides")
    public List<AvailabilityOverrideResponse> listOverrides(
            @PathVariable String staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return availabilityService.listOverrides(staffId, fromDate, toDate);
    }

    @PostMapping("/{staffId}/overrides")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityOverrideResponse createOverride(
            @PathVariable String staffId,
            @Valid @RequestBody OverrideCreateRequest request) {
        return availabilityService.createOverride(staffId, request);
    }

    @PatchMapping("/{staffId}/overrides/{overrideId}")
    public AvailabilityOverrideResponse updateOverride(
            @PathVariable String staffId,
            @PathVariable String overrideId,
            @Valid @RequestBody OverrideUpdateRequest request) {
        return availabilityService.updateOverride(staffId, overrideId, request);
    }

    @DeleteMapping("/{staffId}/overrides/{overrideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOverride(@PathVariable String staffId, @PathVariable String overrideId) {
        availabilityService.deleteOverride(staffId, overrideId);
    }

    @PostMapping("/{staffId}/copy")
    public CopyScheduleResponse copySchedule(
            @PathVariable String staffId,
            @Valid @RequestBody CopyScheduleRequest request) {
        return availabilityService.copySchedule(staffId, request);
    }

    @GetMapping("/{staffId}/timeline")
    public List<TimelineEntryResponse> getAvailabilityTimeline(
            @PathVariable String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.getTimeline(staffId, from, to);
    }

    @GetMapping("/{staffId}/breaks")
    public List<TimeSlotResponse> listBreaks(@PathVariable String staffId) {
        return availabilityService.listBreaks(staffId);
    }

    @PostMapping("/{staffId}/breaks")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeSlotResponse createBreak(
            @PathVariable String staffId,
            @Valid @RequestBody BreakCreateRequest request) {
        return availabilityService.createBreak(staffId, request);
    }

    @DeleteMapping("/{staffId}/breaks/{breakId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBreak(@PathVariable String staffId, @PathVariable String breakId) {
        availabilityService.deleteBreak(staffId, breakId);
    }

    @PostMapping("/bulk-update")
    public BulkAvailabilityUpdateResponse bulkUpdateAvailability(@Valid @RequestBody BulkAvailabilityUpdateRequest request) {
        return availabilityService.bulkUpdateAvailability(request);
    }

    @GetMapping("/holidays")
    public List<HolidayResponse> getAvailabilityHolidays() {
        return availabilityService.getHolidays();
    }
}
