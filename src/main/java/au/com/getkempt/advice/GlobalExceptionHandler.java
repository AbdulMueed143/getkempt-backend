package au.com.getkempt.advice;

import au.com.getkempt.common.error.ApiErrorResponse;
import au.com.getkempt.common.error.FieldViolation;
import au.com.getkempt.common.error.exceptions.ApiException;
import au.com.getkempt.common.error.exceptions.RateLimitedException;
import au.com.getkempt.common.web.RequestContext;
import au.com.getkempt.exceptions.FileUploadException;
import au.com.getkempt.exceptions.InvalidCredentialsException;
import au.com.getkempt.exceptions.JsonParseException;
import au.com.getkempt.exceptions.JwtValidationException;
import au.com.getkempt.exceptions.ResourceAlreadyExistsException;
import au.com.getkempt.exceptions.ResourceNotFoundException;
import au.com.getkempt.exceptions.SessionExpiredException;
import au.com.getkempt.exceptions.UnAuthorizedAccessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Central translation of every exception into the contract's error envelope
 * ({@code Error} / {@code ValidationError}). Controllers and services never build
 * error responses by hand — they throw a typed {@link ApiException} (or let a
 * framework/Mongo exception bubble) and this advice renders it consistently.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String DOCS_BASE = "https://docs.getkempt.co/errors#";

    // ── Our typed API exceptions ─────────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(final ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API error [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        } else {
            log.debug("API error [{}]: {}", ex.getCode(), ex.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof RateLimitedException rl) {
            headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(rl.getRetryAfterSeconds()));
        }

        return ResponseEntity.status(ex.getStatus())
                .headers(headers)
                .body(envelope(ex.getCode(), ex.getMessage(), ex.getFields()));
    }

    // ── Bean-validation on @Valid request bodies → 422 ───────────────────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {

        List<FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(envelope("validation_failed", "One or more fields are invalid", fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(final ConstraintViolationException ex) {
        List<FieldViolation> fields = ex.getConstraintViolations().stream()
                .map(this::toViolation)
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(envelope("validation_failed", "One or more parameters are invalid", fields));
    }

    // ── Mongo concurrency / uniqueness ───────────────────────────────────────

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(final OptimisticLockingFailureException ex) {
        log.debug("Optimistic lock failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .body(envelope("precondition_failed",
                        "The resource was modified by another request. Reload and retry.", null));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateKey(final DuplicateKeyException ex) {
        log.debug("Duplicate key: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(envelope("conflict", "A conflicting record already exists.", null));
    }

    // ── Spring Security ──────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(final AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(envelope("forbidden", "You do not have permission to perform this action.", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(final AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(envelope("unauthorized", "Authentication is required.", null));
    }

    // ── Legacy domain exceptions (pre-existing) ──────────────────────────────

    @ExceptionHandler({ResourceNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleLegacyNotFound(final ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(envelope("not_found", ex.getMessage(), null));
    }

    @ExceptionHandler({ResourceAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleLegacyConflict(final ResourceAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(envelope("conflict", ex.getMessage(), null));
    }

    @ExceptionHandler({UnAuthorizedAccessException.class})
    public ResponseEntity<ApiErrorResponse> handleLegacyForbidden(final UnAuthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(envelope("forbidden", ex.getMessage(), null));
    }

    @ExceptionHandler({JwtValidationException.class, SessionExpiredException.class})
    public ResponseEntity<ApiErrorResponse> handleLegacyUnauthorized(final RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(envelope("unauthorized", ex.getMessage(), null));
    }

    @ExceptionHandler({InvalidCredentialsException.class, JsonParseException.class})
    public ResponseEntity<ApiErrorResponse> handleLegacyBadRequest(final RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(envelope("bad_request", ex.getMessage(), null));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleFileUpload(final FileUploadException ex) {
        log.error("File upload failed: {} ({})", ex.getMessage(), ex.getAdditionalDetails());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(envelope("internal_error", ex.getMessage(), null));
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(final Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(envelope("internal_error", "An unexpected error occurred", null));
    }

    /**
     * Re-render Spring MVC's built-in exceptions (404, 405, 400 unreadable body,
     * missing params, etc.) into our envelope instead of the default ProblemDetail.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            final Exception ex,
            final Object body,
            final HttpHeaders headers,
            final HttpStatusCode statusCode,
            final WebRequest request) {

        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String code = codeForStatus(status);
        String message = (status != null && status.is5xxServerError())
                ? "An unexpected error occurred"
                : ex.getMessage();
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(envelope(code, message, null));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ApiErrorResponse envelope(String code, String message, List<FieldViolation> fields) {
        return ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .requestId(RequestContext.getRequestId())
                .docsUrl(DOCS_BASE + code)
                .fields(fields == null || fields.isEmpty() ? null : fields)
                .build();
    }

    private FieldViolation toViolation(FieldError fieldError) {
        return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getCode());
    }

    private FieldViolation toViolation(ConstraintViolation<?> violation) {
        return new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage(), null);
    }

    private String codeForStatus(HttpStatus status) {
        if (status == null) {
            return "internal_error";
        }
        return switch (status) {
            case BAD_REQUEST -> "bad_request";
            case UNAUTHORIZED -> "unauthorized";
            case PAYMENT_REQUIRED -> "payment_required";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not_found";
            case METHOD_NOT_ALLOWED -> "method_not_allowed";
            case NOT_ACCEPTABLE -> "not_acceptable";
            case CONFLICT -> "conflict";
            case PRECONDITION_FAILED -> "precondition_failed";
            case UNSUPPORTED_MEDIA_TYPE -> "unsupported_media_type";
            case UNPROCESSABLE_ENTITY -> "validation_failed";
            case TOO_MANY_REQUESTS -> "rate_limited";
            default -> status.is5xxServerError() ? "internal_error" : "error";
        };
    }
}
