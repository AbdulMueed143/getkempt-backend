package au.com.getkempt.exceptions;


import lombok.Getter;

@Getter
public class ResourceAlreadyExistsException extends RuntimeException {

    private String additionalDetails;

    public ResourceAlreadyExistsException(final String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }
}
