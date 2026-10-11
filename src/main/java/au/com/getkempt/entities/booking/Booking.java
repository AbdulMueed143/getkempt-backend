package au.com.getkempt.entities.booking;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.booking.BookingSource;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.enums.booking.CancellationReason;
import au.com.getkempt.enums.store.InStoreMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An appointment — the heart of the product ({@code docs/05-booking-lifecycle.md}).
 *
 * <p>{@code clientName}/{@code staffName}/{@code serviceName}/{@code durationMinutes}/
 * {@code price} are snapshotted at creation time and never re-derived from the
 * live Client/StaffMember/Service records, so editing a service's price or
 * renaming a staff member never rewrites booking history (contract note on
 * {@code Booking.serviceName}).
 */
@Document(collection = "bookings")
@Getter
@Setter
public class Booking extends BaseDocument {

    @Indexed
    private String staffId;
    private String staffName;

    private String clientId;
    private String clientName;
    private String clientPhone;
    private String clientEmail;

    private String serviceId;
    private String packageId;
    private String serviceName;

    private int durationMinutes;
    private Money price;

    @Indexed
    private Instant startAt;
    private Instant endAt;

    private BookingStatus status;
    private BookingSource source;
    private String notes;
    private CancellationReason cancellationReason;
    private String cancellationNote;

    private String paymentIntentId;
    private Money depositPaid;
    private boolean paymentCaptured;

    /** Set at completion time when payment was taken in person — see {@code BookingService#complete}. */
    private InStoreMethod inStoreMethodUsed;

    private List<AppliedSurcharge> surchargesApplied = new ArrayList<>();
}
