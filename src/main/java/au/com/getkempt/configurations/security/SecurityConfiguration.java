package au.com.getkempt.configurations.security;

import au.com.getkempt.common.tenant.TenantContextFilter;
import au.com.getkempt.configurations.security.filters.ClientJwtAuthFilter;
import au.com.getkempt.configurations.security.filters.JwtTokenProvider;
import au.com.getkempt.configurations.security.filters.StaffJwtAuthFilter;
import au.com.getkempt.services.authentication.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security topology.
 *
 * <ul>
 *   <li><b>Client chain</b> ({@code /api/v1/client/**}) — the (out-of-scope) customer
 *       OTP surface, kept intact behind its own JWT audience.</li>
 *   <li><b>Business chain</b> ({@code /api/v1/**}) — the staff/owner dashboard API the
 *       OpenAPI contract describes. Stateless bearer JWT; {@code /auth/**} is open,
 *       everything else requires authentication. The {@link TenantContextFilter} runs
 *       right after the JWT filter to bind the tenant for the request.</li>
 *   <li>Docs ({@code /swagger-ui/**}, {@code /openapi.yaml}, {@code /v3/api-docs/**})
 *       match no chain and are served openly.</li>
 * </ul>
 *
 * Auth filters are instantiated here (not {@code @Component}) so they apply only
 * inside their chain and are never auto-registered as global servlet filters.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    @Order(0)
    public SecurityFilterChain clientFilterChain(HttpSecurity http) throws Exception {
        ClientJwtAuthFilter clientJwtAuthFilter = new ClientJwtAuthFilter(jwtTokenProvider);
        return http
                .securityMatcher("/api/v1/client/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/client/auth/otp/request",
                                "/api/v1/client/auth/otp/verify",
                                "/api/v1/client/auth/otp/refresh")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(clientJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain businessFilterChain(HttpSecurity http) throws Exception {
        StaffJwtAuthFilter staffJwtAuthFilter = new StaffJwtAuthFilter(jwtTokenProvider, sessionService);
        TenantContextFilter tenantContextFilter = new TenantContextFilter();
        return http
                .securityMatcher("/api/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/staff/auth/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(staffJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantContextFilter, StaffJwtAuthFilter.class)
                .build();
    }
}
