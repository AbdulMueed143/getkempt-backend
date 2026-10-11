package au.com.getkempt.enums.retail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SaleStatus {

    PENDING("pending"),
    PAID("paid"),
    REFUNDED("refunded"),
    VOIDED("voided");

    private final String value;

    SaleStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SaleStatus from(String raw) {
        for (SaleStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown sale status: " + raw);
    }
}
