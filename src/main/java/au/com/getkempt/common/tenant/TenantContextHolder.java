package au.com.getkempt.common.tenant;

import au.com.getkempt.common.error.exceptions.UnauthorizedException;

import java.util.Optional;

/**
 * Thread-bound holder for the {@link TenantContext} of the in-flight request.
 *
 * <p>Populated once per request by {@code TenantContextFilter} after the JWT is
 * validated, and cleared in a {@code finally} block so threads in the pool never
 * leak tenancy across requests. The {@link #requireShopId()} accessor is the
 * choke-point the data layer calls — it is impossible to run a tenant-scoped
 * query without a resolved shop.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        HOLDER.set(context);
    }

    public static Optional<TenantContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static TenantContext require() {
        TenantContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new UnauthorizedException("No authenticated tenant context on this request");
        }
        return ctx;
    }

    public static String requireShopId() {
        String shopId = require().shopId();
        if (shopId == null || shopId.isBlank()) {
            throw new UnauthorizedException("Authenticated token carries no shop scope");
        }
        return shopId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
