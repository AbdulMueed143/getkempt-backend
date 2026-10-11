package au.com.getkempt.enums.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a retail {@code Sale} was (or will be) paid — the explicit choice a POS
 * checkout makes, one of the two "rails" money can move through on this
 * platform (see {@code docs/06-payments.md}'s Stripe Connect architecture for
 * the online rail; {@code StoreSettings.inStoreMethods} for the in-person one).
 */
public enum PaymentChannel {

    ONLINE("online"),
    IN_STORE("in_store");

    private final String value;

    PaymentChannel(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PaymentChannel from(String raw) {
        for (PaymentChannel channel : values()) {
            if (channel.value.equalsIgnoreCase(raw)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown payment channel: " + raw);
    }
}
