package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How a surcharge is expressed — percentage of price or a fixed AUD amount. */
public enum SurchargeType {

    PERCENTAGE("percentage"),
    FIXED_AUD("fixed_aud");

    private final String value;

    SurchargeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SurchargeType from(String raw) {
        for (SurchargeType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown surcharge type: " + raw);
    }
}
