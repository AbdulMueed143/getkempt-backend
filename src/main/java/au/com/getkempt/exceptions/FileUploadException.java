package au.com.getkempt.exceptions;

import lombok.Getter;

@Getter
public class FileUploadException extends RuntimeException{
    private String additionalDetails;

    public FileUploadException(final String message) {
        super(message);
    }

    public FileUploadException(final String message, final String additionalDetails) {
        super(message);
        this.additionalDetails = additionalDetails;
    }
}
