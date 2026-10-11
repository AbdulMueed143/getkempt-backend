package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Day a recurring surcharge rule applies on. */
public enum SurchargeDay {

    MONDAY("monday"),
    TUESDAY("tuesday"),
    WEDNESDAY("wednesday"),
    THURSDAY("thursday"),
    FRIDAY("friday"),
    SATURDAY("saturday"),
    SUNDAY("sunday");

    private final String value;

    SurchargeDay(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SurchargeDay from(String raw) {
        for (SurchargeDay day : values()) {
            if (day.value.equalsIgnoreCase(raw)) {
                return day;
            }
        }
        throw new IllegalArgumentException("Unknown surcharge day: " + raw);
    }
}
