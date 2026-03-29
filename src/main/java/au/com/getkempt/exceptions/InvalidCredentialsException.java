package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {

    private String additionalDetails;

    public InvalidCredentialsException(final String message) {
        super(message);
    }

    public InvalidCredentialsException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }
}
