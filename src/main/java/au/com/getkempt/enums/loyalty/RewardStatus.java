package au.com.getkempt.enums.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RewardStatus {

    PENDING("pending"),
    REDEEMED("redeemed"),
    EXPIRED("expired"),
    REVOKED("revoked");

    private final String value;

    RewardStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static RewardStatus from(String raw) {
        for (RewardStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown reward status: " + raw);
    }
}
