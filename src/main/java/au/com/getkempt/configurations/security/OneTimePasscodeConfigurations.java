package au.com.getkempt.configurations.security;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.configurations.otp")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OneTimePasscodeConfigurations {

}
