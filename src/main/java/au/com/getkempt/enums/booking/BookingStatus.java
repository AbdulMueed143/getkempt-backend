package au.com.getkempt.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** State machine per {@code docs/05-booking-lifecycle.md}. */
public enum BookingStatus {

    PENDING("pending"),
    CONFIRMED("confirmed"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    NO_SHOW("no_show");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    /** Statuses that still hold a slot (used for overlap/conflict checks). */
    public static final java.util.Set<BookingStatus> ACTIVE = java.util.Set.of(PENDING, CONFIRMED, IN_PROGRESS);

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BookingStatus from(String raw) {
        for (BookingStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown booking status: " + raw);
    }
}
