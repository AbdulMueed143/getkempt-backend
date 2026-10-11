package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** No-show / cancellation fee expression. */
public enum FeeType {

    PERCENTAGE("percentage"),
    FIXED_AUD("fixed_aud"),
    NONE("none");

    private final String value;

    FeeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static FeeType from(String raw) {
        for (FeeType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown fee type: " + raw);
    }
}
