package au.com.getkempt.responses.booking;

import au.com.getkempt.enums.booking.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the contract's {@code AppointmentRecord}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentRecordResponse(
        String id,
        String bookingId,
        String clientId,
        String clientName,
        String serviceName,
        int durationMinutes,
        String staffId,
        String staffName,
        Instant scheduledAt,
        AppointmentStatus status,
        String note,
        Instant statusUpdatedAt,
        String statusUpdatedByStaffId) {
}
