package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private String additionalDetails;

    public ResourceNotFoundException(final String message) {
        super(message);
    }

    public ResourceNotFoundException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }
}
