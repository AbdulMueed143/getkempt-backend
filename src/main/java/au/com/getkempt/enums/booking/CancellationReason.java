package au.com.getkempt.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CancellationReason {

    CLIENT_REQUEST("client_request"),
    STAFF_UNAVAILABLE("staff_unavailable"),
    SHOP_CLOSED("shop_closed"),
    NO_SHOW("no_show"),
    OTHER("other");

    private final String value;

    CancellationReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static CancellationReason from(String raw) {
        for (CancellationReason reason : values()) {
            if (reason.value.equalsIgnoreCase(raw)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown cancellation reason: " + raw);
    }
}
