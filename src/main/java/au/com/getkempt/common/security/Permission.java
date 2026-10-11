package au.com.getkempt.common.security;

/**
 * Fine-grained permission strings embedded in the access token's {@code permissions}
 * claim and checked by services / method security. Roles map to bundles of these
 * (see {@link RolePermissions}) per the RBAC matrix in {@code docs/02-authentication.md}.
 */
public final class Permission {

    private Permission() {
    }

    public static final String BOOKINGS_READ_ALL = "bookings.read.all";
    public static final String BOOKINGS_WRITE = "bookings.write";
    public static final String BOOKINGS_CANCEL = "bookings.cancel";

    public static final String SERVICES_MANAGE = "services.manage";
    public static final String PACKAGES_MANAGE = "packages.manage";

    public static final String STAFF_MANAGE = "staff.manage";
    public static final String STAFF_PAY_MANAGE = "staff.pay.manage";

    public static final String STORE_MANAGE = "store.manage";
    public static final String HOLIDAYS_MANAGE = "holidays.manage";

    public static final String LOYALTY_MANAGE = "loyalty.manage";
    public static final String CAMPAIGNS_DISPATCH = "campaigns.dispatch";
    public static final String REMINDERS_CONFIGURE = "reminders.configure";

    public static final String PAYMENTS_MANAGE = "payments.manage";
    public static final String BILLING_MANAGE = "billing.manage";

    public static final String CLIENTS_READ_ALL = "clients.read.all";
    public static final String CLIENTS_EDIT = "clients.edit";

    // --- retail (products / POS sales) ---
    public static final String PRODUCTS_MANAGE = "products.manage";
    public static final String SALES_CREATE = "sales.create";
    public static final String SALES_READ_ALL = "sales.read.all";
}
