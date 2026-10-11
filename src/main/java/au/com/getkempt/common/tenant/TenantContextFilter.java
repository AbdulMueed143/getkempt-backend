package au.com.getkempt.common.tenant;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Populates {@link TenantContextHolder} from the validated JWT once authentication
 * has been established by the upstream auth filter, then clears it after the request.
 *
 * <p>This is the ONLY place the tenant is derived, and it is derived solely from the
 * token's claims — never from a body, header, or URL — which is the contract in
 * {@code docs/03-multi-tenancy.md}. It is wired inside each security filter chain
 * (right after the JWT filter), not auto-registered globally, so ordering is exact.
 */
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof Claims claims) {
                Set<String> permissions = Set.of();
                Object rawPermissions = claims.get("permissions");
                if (rawPermissions instanceof List<?> list) {
                    permissions = list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
                }

                TenantContextHolder.set(TenantContext.builder()
                        .userId(claims.getSubject())
                        .shopId(claims.get("shop_id", String.class))
                        .staffId(claims.get("staff_id", String.class))
                        .role(claims.get("role", String.class))
                        .permissions(permissions)
                        .sessionId(claims.get("jti", String.class))
                        .build());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
