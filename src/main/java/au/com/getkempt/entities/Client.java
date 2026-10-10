package au.com.getkempt.entities;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A shop's customer record. Created explicitly ({@code POST /clients}) or inline
 * from a booking's {@code client} payload — see {@code docs/05-booking-lifecycle.md}
 * step 3 ("resolve or create client: upsert by phone").
 *
 * <p>{@code totalBookings} / {@code lifetimeSpendCents} / {@code lastBookingDate} /
 * {@code nextBookingDate} are denormalised counters maintained by the Bookings
 * domain (incremented on completion, recomputed on cancel/reschedule) so the client
 * list can render them without a join on every page load.
 */
@Document(collection = "clients")
@Getter
@Setter
public class Client extends BaseDocument {

    private String firstName;
    private String lastName;
    private String email;

    @Indexed
    private String phone;

    private int totalBookings;
    private long lifetimeSpendCents;
    private Instant lastBookingDate;
    private Instant nextBookingDate;
    private String preferredStaffId;
    private Instant joinedAt;

    private boolean marketingOptIn = true;
    private boolean smsOptIn = true;
    private boolean emailOptIn = true;

    /** One-line summary shown on the client card — distinct from {@link #notes}. */
    private String summary;

    private List<String> tags = new ArrayList<>();

    /** Full notes thread; sub-resource {@code /clients/{id}/notes}. */
    private List<ClientNote> notes = new ArrayList<>();
}
