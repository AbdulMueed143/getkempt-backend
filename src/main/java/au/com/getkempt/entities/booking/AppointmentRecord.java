package au.com.getkempt.entities.booking;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.booking.AppointmentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A materialised, 1:1 read-model of a {@link Booking} for the Visits page —
 * per {@code paths/visits.yaml}, kept as its own collection "so the Visits page
 * can be queried without scanning the full bookings table".
 *
 * <p>The contract's docs describe this being (re)materialised by a daily job;
 * this codebase has no job scheduler (see {@code BookingService} class javadoc),
 * so instead it is created at booking-creation time and updated synchronously
 * by {@code BookingService} exactly when the underlying booking transitions
 * (complete / no-show / cancel / reschedule) — arguably more correct than a
 * batch job anyway, since there's no replication lag.
 */
@Document(collection = "appointment_records")
@Getter
@Setter
public class AppointmentRecord extends BaseDocument {

    @Indexed(unique = true)
    private String bookingId;

    private String clientId;
    private String clientName;
    private String serviceName;
    private int durationMinutes;

    @Indexed
    private String staffId;
    private String staffName;

    @Indexed
    private Instant scheduledAt;

    private AppointmentStatus status;
    private String note;
    private Instant statusUpdatedAt;
    private String statusUpdatedByStaffId;
}
