package au.com.getkempt.configurations.security;

import au.com.getkempt.common.error.ErrorJson;
import au.com.getkempt.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renders authenticated-but-forbidden failures (403) using the standard error
 * envelope. Reached only when the caller IS authenticated for the tenant but lacks
 * the role/permission — cross-tenant access uses 404, never 403.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ErrorJson.render(
                "forbidden",
                "You do not have permission to perform this action.",
                RequestContext.getRequestId(),
                "https://docs.getkempt.co/errors#forbidden"));
    }
}
