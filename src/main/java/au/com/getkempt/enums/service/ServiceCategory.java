package au.com.getkempt.enums.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Canonical service category. */
public enum ServiceCategory {

    HAIR("hair"),
    BARBER("barber"),
    NAILS("nails"),
    BEAUTY("beauty"),
    LASH_BROW("lash_brow"),
    MASSAGE("massage"),
    OTHER("other");

    private final String value;

    ServiceCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ServiceCategory from(String raw) {
        for (ServiceCategory c : values()) {
            if (c.value.equalsIgnoreCase(raw)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown service category: " + raw);
    }
}
