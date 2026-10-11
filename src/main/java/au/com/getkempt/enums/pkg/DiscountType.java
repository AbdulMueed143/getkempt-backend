package au.com.getkempt.enums.pkg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Package discount expression — percentage of base price or a fixed AUD amount. */
public enum DiscountType {

    PERCENTAGE("percentage"),
    FIXED("fixed");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static DiscountType from(String raw) {
        for (DiscountType d : values()) {
            if (d.value.equalsIgnoreCase(raw)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown discount type: " + raw);
    }
}
