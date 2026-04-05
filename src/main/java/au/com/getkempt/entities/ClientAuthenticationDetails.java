package au.com.getkempt.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "staff_authentication_details")
public class ClientAuthenticationDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String staffId;

    private String staffShopId;
    private String email;
    private String os;
    private String osVersion;
    private String browser;
    private String device;
    private String browserVersion;
    private String category;


}
