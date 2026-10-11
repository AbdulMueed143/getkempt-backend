package au.com.getkempt.services.outbox;

import au.com.getkempt.configurations.security.properties.OutboxProperties;
import au.com.getkempt.entities.outbox.OutboxMessage;
import au.com.getkempt.integrations.email.EmailSender;
import au.com.getkempt.integrations.push.PushSender;
import au.com.getkempt.integrations.sms.SmsSender;
import au.com.getkempt.repositories.outbox.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The other half of the outbox pattern: a background poller that drains
 * {@code PENDING} rows and performs the actual provider call, completely off
 * the request thread that enqueued them (see {@code OutboxService} javadoc).
 *
 * <p>Failure handling is exponential backoff up to {@code maxAttempts}, then the
 * row is marked {@code DEAD} and left in place for manual inspection (no
 * alerting-on-alerting-failure exists yet — a reasonable v2 would raise a
 * Dashboard alert / bell notification for shops with DEAD rows, mirroring the
 * "reminder queue out_of_credits" alert the docs describe for SMS).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {

    private final OutboxMessageRepository repository;
    private final OutboxProperties properties;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushSender pushSender;

    @Scheduled(fixedDelayString = "${application.configurations.outbox.poll-interval-ms}")
    public void dispatch() {
        List<OutboxMessage> batch = repository.claimBatch(properties.getBatchSize());
        for (OutboxMessage message : batch) {
            dispatchOne(message);
        }
    }

    private void dispatchOne(OutboxMessage message) {
        try {
            String providerMessageId = switch (message.getChannel()) {
                case EMAIL -> emailSender.send(message.getRecipient(), message.getSubject(), message.getBody()).providerMessageId();
                case SMS -> smsSender.send(message.getRecipient(), message.getBody()).providerMessageId();
                case PUSH -> pushSender.send(message.getRecipient(), message.getSubject(), message.getBody(), Map.of())
                        .providerMessageId();
            };
            repository.markSent(message.getId(), providerMessageId);
        } catch (Exception e) {
            int attempts = message.getAttempts() + 1;
            boolean dead = attempts >= Math.max(1, message.getMaxAttempts());
            Instant nextAttemptAt = dead ? null : Instant.now().plusSeconds(backoffSeconds(attempts));
            repository.markFailed(message.getId(), attempts, e.getMessage(), nextAttemptAt, dead);
            log.warn("[outbox] {} send failed (channel={} attempt={}/{} dead={}): {}",
                    message.getId(), message.getChannel(), attempts, message.getMaxAttempts(), dead, e.getMessage());
        }
    }

    /** Doubling backoff from {@code baseBackoffSeconds}, capped at {@code maxBackoffSeconds}. */
    private long backoffSeconds(int attempt) {
        long uncapped = properties.getBaseBackoffSeconds() * (1L << Math.min(attempt, 20));
        return Math.min(properties.getMaxBackoffSeconds(), uncapped);
    }
}
