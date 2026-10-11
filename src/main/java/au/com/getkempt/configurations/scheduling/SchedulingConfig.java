package au.com.getkempt.configurations.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} beans. Previously nothing in this codebase used a
 * scheduler at all — every background-job-shaped requirement in the docs
 * (auto no-show transitions, reminder dispatch, ...) was instead handled
 * synchronously at the relevant explicit endpoint. The Outbox dispatcher is the
 * first genuine background worker, so this is the first place scheduling gets
 * switched on; it's kept as its own tiny config class (rather than folded into
 * {@code GetkemptApplication}) so it reads as a deliberate, singular decision.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
