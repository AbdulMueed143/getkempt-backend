package au.com.getkempt.enums.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Shop business category — {@code StoreProfile.businessType}. */
public enum BusinessType {

    BARBERSHOP("barbershop"),
    HAIR_SALON("hair_salon"),
    NAIL_SALON("nail_salon"),
    BEAUTY_SALON("beauty_salon"),
    DAY_SPA("day_spa"),
    MASSAGE_STUDIO("massage_studio"),
    WELLNESS_CENTRE("wellness_centre"),
    COSMETIC_STUDIO("cosmetic_studio"),
    OTHER("other");

    private final String value;

    BusinessType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BusinessType from(String raw) {
        for (BusinessType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown business type: " + raw);
    }
}
