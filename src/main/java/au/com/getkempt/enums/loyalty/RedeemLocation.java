package au.com.getkempt.enums.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RedeemLocation {

    OWN_SHOP("own_shop"),
    PARTNER("partner");

    private final String value;

    RedeemLocation(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static RedeemLocation from(String raw) {
        for (RedeemLocation location : values()) {
            if (location.value.equalsIgnoreCase(raw)) {
                return location;
            }
        }
        throw new IllegalArgumentException("Unknown redeem location: " + raw);
    }
}
