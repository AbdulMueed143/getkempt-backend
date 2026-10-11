package au.com.getkempt.enums.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TriggerType {

    VISITS("visits"),
    SPEND("spend");

    private final String value;

    TriggerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static TriggerType from(String raw) {
        for (TriggerType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown trigger type: " + raw);
    }
}
