package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Accepted in-store payment methods. */
public enum InStoreMethod {

    CARD("card"),
    CASH("cash"),
    BANK_TRANSFER("bank_transfer");

    private final String value;

    InStoreMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static InStoreMethod from(String raw) {
        for (InStoreMethod method : values()) {
            if (method.value.equalsIgnoreCase(raw)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown in-store method: " + raw);
    }
}
