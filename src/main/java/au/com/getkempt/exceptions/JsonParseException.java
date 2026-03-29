package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class JsonParseException extends RuntimeException {

    private String additionalDetails;

    public JsonParseException(final String message) {
        super(message);
    }

    public JsonParseException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }
}
