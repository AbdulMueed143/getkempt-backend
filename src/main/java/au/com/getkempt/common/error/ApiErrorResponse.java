package au.com.getkempt.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * The single error envelope returned by every failing endpoint — matches the
 * {@code Error} / {@code ValidationError} schemas in the OpenAPI contract
 * ({@code docs/01-conventions.md §9}).
 *
 * <pre>
 * {
 *   "code":      "not_found",
 *   "message":   "Booking with id bk_… not found",
 *   "requestId": "req_…",
 *   "docsUrl":   "https://docs.getkempt.co/errors#not_found",
 *   "fields":    [ { "field": "email", "message": "Must be a valid email" } ]
 * }
 * </pre>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        String message,
        String requestId,
        String docsUrl,
        List<FieldViolation> fields) {
}
