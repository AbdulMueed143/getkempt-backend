package au.com.getkempt.configurations.security.filters;

import au.com.getkempt.enums.CallerType;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ClientJwtAuthFilter extends AbstractJwtAuthFilter {

    public ClientJwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        super(jwtTokenProvider);
    }

    @Override
    protected CallerType getAudience() {
        return CallerType.CLIENT;
    }

    @Override
    protected List<SimpleGrantedAuthority> buildAuthorities(Claims claim) {
        return List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }
}
