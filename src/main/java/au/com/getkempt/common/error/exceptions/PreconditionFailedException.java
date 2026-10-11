package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/** 412 — optimistic-lock {@code If-Match} mismatch; the entity changed concurrently. */
public class PreconditionFailedException extends ApiException {

    public PreconditionFailedException(String message) {
        super(HttpStatus.PRECONDITION_FAILED, "precondition_failed", message);
    }
}
