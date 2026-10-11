package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Deposit expression for the {@code deposit} payment mode. */
public enum DepositType {

    PERCENTAGE("percentage"),
    FIXED_AUD("fixed_aud");

    private final String value;

    DepositType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static DepositType from(String raw) {
        for (DepositType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown deposit type: " + raw);
    }
}
