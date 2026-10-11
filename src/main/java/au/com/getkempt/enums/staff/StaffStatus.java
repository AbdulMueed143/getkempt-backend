package au.com.getkempt.enums.staff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Employment status of a {@code StaffMember}. */
public enum StaffStatus {

    ACTIVE("active"),
    ON_LEAVE("on_leave"),
    INACTIVE("inactive");

    private final String value;

    StaffStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static StaffStatus from(String raw) {
        for (StaffStatus s : values()) {
            if (s.value.equalsIgnoreCase(raw)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown staff status: " + raw);
    }
}
