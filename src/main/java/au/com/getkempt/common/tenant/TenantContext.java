package au.com.getkempt.common.tenant;

import lombok.Builder;

import java.util.Set;

/**
 * The authenticated caller's tenant + identity, derived EXCLUSIVELY from the
 * validated JWT (never from a request body, header, or URL) — per
 * {@code docs/03-multi-tenancy.md}.
 *
 * @param shopId      the tenant; every tenant-scoped query filters on this
 * @param userId      the {@code User.id} (JWT {@code sub})
 * @param staffId     the linked staff member, when the user is staff (nullable)
 * @param role        coarse role: {@code owner} / {@code manager} / {@code staff} / {@code admin}
 * @param permissions fine-grained permission strings (e.g. {@code bookings.write})
 * @param sessionId   the session id ({@code jti}) used for revocation
 */
@Builder
public record TenantContext(
        String shopId,
        String userId,
        String staffId,
        String role,
        Set<String> permissions,
        String sessionId) {

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isOwner() {
        return "owner".equalsIgnoreCase(role);
    }
}
