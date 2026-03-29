package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class JwtValidationException extends RuntimeException{
    private String additionalDetails;

    public JwtValidationException(final String message) {
        super(message);
    }

    public JwtValidationException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }

    public JwtValidationException(final String message, final String additionalDetails, final Throwable cause) {
        super(message, cause);
        this.additionalDetails = additionalDetails;
    }
}
