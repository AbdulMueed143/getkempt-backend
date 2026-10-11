package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/** 401 — missing, expired, or revoked access token. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "unauthorized", message);
    }
}
