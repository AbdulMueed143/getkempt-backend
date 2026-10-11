package au.com.getkempt.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Review status of an {@code AppointmentRecord} — distinct from {@link BookingStatus}:
 * a booking can be {@code confirmed} while its appointment sits {@code pending}
 * review right up until staff explicitly marks it attended/no-show, or the
 * (not-yet-built) auto-no-show job would have done so 30 min after {@code endAt}.
 */
public enum AppointmentStatus {

    PENDING("pending"),
    COMPLETED("completed"),
    NO_SHOW("no_show"),
    CANCELLED("cancelled");

    private final String value;

    AppointmentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static AppointmentStatus from(String raw) {
        for (AppointmentStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown appointment status: " + raw);
    }
}
