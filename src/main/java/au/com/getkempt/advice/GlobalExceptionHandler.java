package au.com.getkempt.advice;


import au.com.getkempt.exceptions.FileUploadException;
import au.com.getkempt.exceptions.InvalidCredentialsException;
import au.com.getkempt.responses.errors.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(final FileUploadException exception) {
        log.error("message: {}, additionalDetails: {}", exception.getMessage(), exception.getAdditionalDetails());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message(exception.getMessage())
                        .additionalDetail(exception.getAdditionalDetails())
                        .build()
        );
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(final InvalidCredentialsException exception) {
        log.error("message: {}, additionalDetails: {}", exception.getMessage(), exception.getAdditionalDetails());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message(exception.getMessage())
                        .additionalDetail(exception.getAdditionalDetails())
                        .build()
        );
    }

}
