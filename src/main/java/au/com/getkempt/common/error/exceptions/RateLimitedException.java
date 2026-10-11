package au.com.getkempt.common.error.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 429 — rate limit exceeded. Carries the {@code Retry-After} hint and the name of
 * the bucket that tripped, both surfaced in the response per {@code 08-rate-limiting.md}.
 */
@Getter
public class RateLimitedException extends ApiException {

    private final int retryAfterSeconds;
    private final String bucket;

    public RateLimitedException(String message, int retryAfterSeconds, String bucket) {
        super(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.bucket = bucket;
    }
}
