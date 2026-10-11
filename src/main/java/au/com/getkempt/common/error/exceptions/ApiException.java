package au.com.getkempt.common.error.exceptions;

import au.com.getkempt.common.error.FieldViolation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base type for all deliberately-thrown API errors.
 *
 * <p>Each subclass binds a fixed {@link HttpStatus} and a stable, snake_case
 * {@code code} that the frontend can switch on. The {@code GlobalExceptionHandler}
 * renders any {@code ApiException} into the standard error envelope, so business
 * code never builds {@code ResponseEntity}s by hand.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient List<FieldViolation> fields;

    protected ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    protected ApiException(HttpStatus status, String code, String message, List<FieldViolation> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }
}
