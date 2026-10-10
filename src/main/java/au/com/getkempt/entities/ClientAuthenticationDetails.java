package au.com.getkempt.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Device / session details captured when a <em>client</em> (customer) authenticates.
 * Placeholder for the out-of-scope customer OTP surface; nothing persists to it yet.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "client_authentication_details")
public class ClientAuthenticationDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String clientId;

    private String clientShopId;
    private String email;
    private String os;
    private String osVersion;
    private String browser;
    private String device;
    private String browserVersion;
    private String category;


}
