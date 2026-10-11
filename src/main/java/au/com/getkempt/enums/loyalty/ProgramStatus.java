package au.com.getkempt.enums.loyalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProgramStatus {

    ACTIVE("active"),
    INACTIVE("inactive"),
    DRAFT("draft");

    private final String value;

    ProgramStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ProgramStatus from(String raw) {
        for (ProgramStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown program status: " + raw);
    }
}
