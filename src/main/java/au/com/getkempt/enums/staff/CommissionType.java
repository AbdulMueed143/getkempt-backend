package au.com.getkempt.enums.staff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How a staff member is paid; changes the meaning of {@code commissionRate}. */
public enum CommissionType {

    HOURLY("hourly"),
    COMMISSION("commission"),
    SALARY("salary");

    private final String value;

    CommissionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static CommissionType from(String raw) {
        for (CommissionType c : values()) {
            if (c.value.equalsIgnoreCase(raw)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown commission type: " + raw);
    }
}
