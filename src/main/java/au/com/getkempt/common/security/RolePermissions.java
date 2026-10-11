package au.com.getkempt.common.security;

import java.util.Set;

import static au.com.getkempt.common.security.Permission.*;

/**
 * Static role → permission-bundle mapping (the RBAC matrix from
 * {@code docs/02-authentication.md}). Permissions are computed at token-mint time
 * and baked into the {@code permissions} claim so request-time checks need no lookup.
 */
public final class RolePermissions {

    private RolePermissions() {
    }

    private static final Set<String> OWNER = Set.of(
            BOOKINGS_READ_ALL, BOOKINGS_WRITE, BOOKINGS_CANCEL,
            SERVICES_MANAGE, PACKAGES_MANAGE,
            STAFF_MANAGE, STAFF_PAY_MANAGE,
            STORE_MANAGE, HOLIDAYS_MANAGE,
            LOYALTY_MANAGE, CAMPAIGNS_DISPATCH, REMINDERS_CONFIGURE,
            PAYMENTS_MANAGE, BILLING_MANAGE,
            CLIENTS_READ_ALL, CLIENTS_EDIT,
            PRODUCTS_MANAGE, SALES_CREATE, SALES_READ_ALL);

    private static final Set<String> MANAGER = Set.of(
            BOOKINGS_READ_ALL, BOOKINGS_WRITE, BOOKINGS_CANCEL,
            SERVICES_MANAGE, PACKAGES_MANAGE,
            STAFF_MANAGE,
            HOLIDAYS_MANAGE,
            LOYALTY_MANAGE, CAMPAIGNS_DISPATCH, REMINDERS_CONFIGURE,
            CLIENTS_READ_ALL, CLIENTS_EDIT,
            PRODUCTS_MANAGE, SALES_CREATE, SALES_READ_ALL);

    private static final Set<String> STAFF = Set.of(
            BOOKINGS_WRITE, SALES_CREATE);

    public static Set<String> forRole(StaffRole role) {
        return switch (role) {
            case OWNER, ADMIN -> OWNER;
            case MANAGER -> MANAGER;
            case STAFF -> STAFF;
        };
    }
}
