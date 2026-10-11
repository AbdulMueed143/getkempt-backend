package au.com.getkempt.configurations.security;

import au.com.getkempt.common.error.ErrorJson;
import au.com.getkempt.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renders missing/invalid-token failures (401) using the standard error envelope,
 * so security-layer rejections are indistinguishable in shape from any other API
 * error. Without this, Spring Boot would emit its default error JSON.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ErrorJson.render(
                "unauthorized",
                "Authentication is required to access this resource.",
                RequestContext.getRequestId(),
                "https://docs.getkempt.co/errors#unauthorized"));
    }
}
