package au.com.getkempt.entities.booking;

import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB's answer to the Postgres {@code EXCLUDE USING gist} constraint the
 * contract's docs describe for race-safe booking creation ({@code
 * docs/05-booking-lifecycle.md} "Race condition handling") — Mongo has no range
 * type / exclusion constraint, so we discretise each booking's time range into
 * fixed-size buckets ({@code application.configurations.booking.slot-lock-bucket-minutes})
 * and insert one lock row per bucket, per staff member, inside the SAME
 * transaction as the booking write (see {@code BookingService#createBooking}).
 *
 * <p>The unique index below is what actually enforces exclusivity: if two
 * concurrent requests both try to claim an overlapping bucket, the second
 * insert raises a {@code DuplicateKeyException}, the whole transaction aborts,
 * and the handler translates that into {@code 409 Conflict} — exactly mirroring
 * the Postgres exclusion-constraint behaviour the docs describe, just
 * implemented as N narrow unique rows instead of one range-typed one.
 */
@Document(collection = "booking_slot_locks")
@CompoundIndex(name = "uniq_shop_staff_bucket", def = "{'shopId': 1, 'staffId': 1, 'bucketStart': 1}", unique = true)
@Getter
@Setter
public class SlotLock extends BaseDocument {

    private String staffId;
    private Instant bucketStart;
    private String bookingId;
}
