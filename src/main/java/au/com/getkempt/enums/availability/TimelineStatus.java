package au.com.getkempt.enums.availability;

import com.fasterxml.jackson.annotation.JsonValue;

/** Per-day classification returned by {@code GET /availability/{staffId}/timeline}. */
public enum TimelineStatus {

    OFF("off"),
    WORKING("working"),
    PARTIAL("partial"),
    OVERRIDE("override");

    private final String value;

    TimelineStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
