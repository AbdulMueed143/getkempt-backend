package au.com.getkempt.responses.errors;

import lombok.*;
import org.springframework.http.HttpStatus;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private HttpStatus status;
    private String message;
    private int statusCode;

    // @Singular handles everything your custom constructors were trying to do
    @Singular
    private Set<String> additionalDetails;
}
