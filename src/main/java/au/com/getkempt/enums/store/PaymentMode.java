package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How the shop takes payment at booking time. */
public enum PaymentMode {

    IN_STORE("in_store"),
    DEPOSIT("deposit"),
    FULL_UPFRONT("full_upfront");

    private final String value;

    PaymentMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PaymentMode from(String raw) {
        for (PaymentMode mode : values()) {
            if (mode.value.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown payment mode: " + raw);
    }
}
