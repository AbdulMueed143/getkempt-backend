package au.com.getkempt.configurations.security.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Tuning for the Outbox dispatcher. {@code pollIntervalMs} itself is read
 * directly by the {@code @Scheduled(fixedDelayString = ...)} annotation (Spring
 * needs a property placeholder there, not a bean), so it is not duplicated here.
 */
@Configuration
@Data
public class OutboxProperties {

    @Value("${application.configurations.outbox.batch-size}")
    private int batchSize;

    @Value("${application.configurations.outbox.max-attempts}")
    private int maxAttempts;

    @Value("${application.configurations.outbox.base-backoff-seconds}")
    private long baseBackoffSeconds;

    @Value("${application.configurations.outbox.max-backoff-seconds}")
    private long maxBackoffSeconds;
}
