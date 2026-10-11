package au.com.getkempt.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingSource {

    APP("app"),
    MANUAL("manual"),
    WALK_IN("walk_in"),
    ONLINE_WIDGET("online_widget"),
    RECURRING("recurring");

    private final String value;

    BookingSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BookingSource from(String raw) {
        for (BookingSource source : values()) {
            if (source.value.equalsIgnoreCase(raw)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown booking source: " + raw);
    }
}
