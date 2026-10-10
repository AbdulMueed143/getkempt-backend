package au.com.getkempt.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JWTRequest {

    @NotEmpty(message = "Id can not be empty")
    private String id;

    private Set<String> roles;
    private Date jwtExpiryDate;
}
