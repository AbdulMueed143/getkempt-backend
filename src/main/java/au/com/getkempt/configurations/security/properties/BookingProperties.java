package au.com.getkempt.configurations.security.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class BookingProperties {

    @Value("${application.configurations.booking.slot-lock-bucket-minutes}")
    private int slotLockBucketMinutes;
}
