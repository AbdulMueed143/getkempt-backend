package au.com.getkempt.configurations.security.filters;

import au.com.getkempt.enums.CallerType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class AbstractJwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    protected AbstractJwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    //We need specific implementation for this one
    protected abstract CallerType getAudience();

    /**
     * Build granted authorities from the validated claims.
     * Staff tokens might carry granular permissions; client tokens get ROLE_CLIENT.
     */
    protected abstract List<SimpleGrantedAuthority> buildAuthorities(Claims claim);

    /**
     * Revocation hook: even a cryptographically-valid token must be rejected if its
     * session ({@code jti}) has been logged out or expired server-side. Default allows
     * all; the staff filter overrides this to consult the session store.
     */
    protected boolean isSessionActive(Claims claims) {
        return true;
    }



    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            final Claims claims = tokenProvider.parseAndValidateForAudience(jwt, getAudience());

            // Reject refresh tokens used as access tokens
            if (tokenProvider.isRefreshToken(claims)) {
                log.warn("Refresh token presented as access token, sub={}", claims.getSubject());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token type");
                return;
            }

            // Server-side revocation: a logged-out / expired session invalidates the token.
            if (!isSessionActive(claims)) {
                log.debug("Rejecting token for revoked/expired session, sub={}", claims.getSubject());
                filterChain.doFilter(request, response);
                return;
            }

            final String subject = tokenProvider.extractSubject(claims);
            final List<SimpleGrantedAuthority> authorities = buildAuthorities(claims);

            final UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, claims, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.debug("JWT validation failed for {} filter: {}", getAudience(), e.getMessage());
            // Don't set authentication — Spring Security handles the 401
        }

        filterChain.doFilter(request, response);

    }
}
