package au.com.getkempt.responses.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardAlertResponse(
        String id,
        String severity,
        String title,
        String description,
        Instant time,
        String actionLabel,
        String actionHref,
        boolean read) {
}
