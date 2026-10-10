package au.com.getkempt.configurations.security.filters;


import au.com.getkempt.enums.CallerType;
import au.com.getkempt.services.authentication.SessionService;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

public class StaffJwtAuthFilter extends AbstractJwtAuthFilter {

    private final SessionService sessionService;

    public StaffJwtAuthFilter(JwtTokenProvider jwtTokenProvider, SessionService sessionService) {
        super(jwtTokenProvider);
        this.sessionService = sessionService;
    }

    @Override
    protected CallerType getAudience() {
        return CallerType.STAFF;
    }

    @Override
    protected boolean isSessionActive(Claims claims) {
        String sessionId = claims.get("jti", String.class);
        // No jti (legacy/non-session token) → fall back to signature/expiry validity.
        return sessionId == null || sessionService.isSessionActive(sessionId);
    }

    @Override
    protected List<SimpleGrantedAuthority> buildAuthorities(Claims claims) {
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));

        // Staff tokens carry a "permissions" claim: ["MANAGE_BOOKINGS", "VIEW_REPORTS", ...]
        final List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // Staff tokens carry a "role" claim for org-level role: OWNER, MANAGER, EMPLOYEE
        final String orgRole = claims.get("role", String.class);
        if (orgRole != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + orgRole.toUpperCase()));
        }

        return authorities;
    }
}
