package au.com.getkempt.responses.booking;

import au.com.getkempt.responses.store.HolidayResponse;

import java.time.Instant;
import java.util.List;

public record CalendarResponse(List<BookingResponse> bookings, List<BreakBlock> breaks, List<HolidayResponse> holidays) {

    public record BreakBlock(String staffId, Instant startAt, Instant endAt, String label) {
    }
}
