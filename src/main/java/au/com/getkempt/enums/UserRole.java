package au.com.getkempt.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Account-level role ({@code User.role}) — distinct from the per-shop {@code StaffRole}.
 * Wire format is snake_case per {@code docs/01-conventions.md §3}.
 */
public enum UserRole {

    BARBER("barber"),
    SALON_OWNER("salon_owner"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static UserRole from(String raw) {
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(raw)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + raw);
    }
}
