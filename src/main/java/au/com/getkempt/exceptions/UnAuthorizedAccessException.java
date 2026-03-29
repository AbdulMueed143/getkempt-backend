package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class UnAuthorizedAccessException extends RuntimeException {

    private String additionalDetails;

    public UnAuthorizedAccessException(final String message) {
        super(message);
    }

    public UnAuthorizedAccessException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }

}
