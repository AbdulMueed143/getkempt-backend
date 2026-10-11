package au.com.getkempt.enums.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RewardType {

    PERCENTAGE("percentage"),
    FIXED("fixed"),
    FREE_VISIT("free_visit"),
    FREE_ITEM("free_item");

    private final String value;

    RewardType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static RewardType from(String raw) {
        for (RewardType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reward type: " + raw);
    }
}
