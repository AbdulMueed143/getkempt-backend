package au.com.getkempt.configurations.security.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class JwtProperties {

    @Value("${application.configurations.jwt.secret}")
    private String secret;

    @Value("${application.configurations.jwt.staff-token-ttl-minutes}")
    private int staffTokenTTL;

    @Value("${application.configurations.jwt.client-token-ttl-minutes}")
    private int clientTokenTTL;

    @Value("${application.configurations.jwt.refresh-token-ttl-days}")
    private int refreshTokenTTLInDays;

    @Value("${application.configurations.jwt.issuer}")
    private String issuer;

}
