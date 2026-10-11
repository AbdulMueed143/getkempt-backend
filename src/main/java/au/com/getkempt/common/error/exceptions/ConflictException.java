package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 409 — duplicate, slot already taken, or an illegal state-machine transition
 * (e.g. confirming a completed booking).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "conflict", message);
    }
}
