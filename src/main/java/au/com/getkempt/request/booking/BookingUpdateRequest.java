package au.com.getkempt.request.booking;

import java.time.Instant;

/**
 * PATCH body for {@code /bookings/{id}}. Per the contract's own guidance, time/staff
 * changes belong on {@code /reschedule} — see {@code BookingService#update} for why
 * {@code staffId}/{@code startAt} are rejected here rather than silently duplicating
 * reschedule's conflict-checking in a second code path.
 */
public record BookingUpdateRequest(String staffId, Instant startAt, String serviceId, String notes) {
}
