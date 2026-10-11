package au.com.getkempt.common.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Coarse per-shop role carried in the token's {@code role} claim
 * ({@code docs/02-authentication.md}) and on {@code StaffMember.role}. Distinct from
 * the account-level {@code UserRole}. Wire form is lower-case ({@code owner} ...).
 * {@code ADMIN} is internal-only and never appears in the contract's StaffRole enum.
 */
public enum StaffRole {

    OWNER,
    MANAGER,
    STAFF,
    ADMIN;

    @JsonValue
    public String wire() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static StaffRole from(String raw) {
        for (StaffRole role : values()) {
            if (role.name().equalsIgnoreCase(raw)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown staff role: " + raw);
    }
}
