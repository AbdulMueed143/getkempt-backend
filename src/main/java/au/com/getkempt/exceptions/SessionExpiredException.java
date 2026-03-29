package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class SessionExpiredException extends RuntimeException{
    private String additionalDetails;

    public SessionExpiredException(final String message) {
        super(message);
    }

    public SessionExpiredException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }

    public SessionExpiredException(final String message, final String additionalDetails, final Throwable cause) {
        super(message, cause);
        this.additionalDetails = additionalDetails;
    }
}
