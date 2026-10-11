package au.com.getkempt.common.error.exceptions;

import au.com.getkempt.common.error.FieldViolation;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 422 — the body parsed as valid JSON but failed business validation. Carries the
 * per-field violations rendered into the {@code ValidationError} envelope.
 */
public class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(String message, List<FieldViolation> fields) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "validation_failed", message, fields);
    }

    public UnprocessableEntityException(String message) {
        this(message, List.of());
    }
}
