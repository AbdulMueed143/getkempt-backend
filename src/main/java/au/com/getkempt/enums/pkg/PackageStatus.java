package au.com.getkempt.enums.pkg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lifecycle status of a package. */
public enum PackageStatus {

    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    private final String value;

    PackageStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PackageStatus from(String raw) {
        for (PackageStatus s : values()) {
            if (s.value.equalsIgnoreCase(raw)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown package status: " + raw);
    }
}
