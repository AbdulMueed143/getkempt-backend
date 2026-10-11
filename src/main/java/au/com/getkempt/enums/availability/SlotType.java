package au.com.getkempt.enums.availability;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Whether a {@code TimeSlot} is bookable working time or a non-bookable break. */
public enum SlotType {

    WORK("work"),
    BREAK("break");

    private final String value;

    SlotType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SlotType from(String raw) {
        for (SlotType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown slot type: " + raw);
    }
}
