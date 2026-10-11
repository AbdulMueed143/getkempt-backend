package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Unit for how far ahead clients may book ({@code StoreSettings.bookingWindowUnit}). */
public enum BookingWindowUnit {

    DAYS("days"),
    WEEKS("weeks"),
    MONTHS("months");

    private final String value;

    BookingWindowUnit(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BookingWindowUnit from(String raw) {
        for (BookingWindowUnit unit : values()) {
            if (unit.value.equalsIgnoreCase(raw)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown booking window unit: " + raw);
    }
}
