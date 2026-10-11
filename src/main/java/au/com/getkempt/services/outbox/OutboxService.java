package au.com.getkempt.services.outbox;

import au.com.getkempt.configurations.security.properties.OutboxProperties;
import au.com.getkempt.entities.outbox.OutboxMessage;
import au.com.getkempt.enums.outbox.OutboxChannel;
import au.com.getkempt.repositories.outbox.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Enqueue-only front door to the transactional outbox. Every place in the
 * codebase that used to call {@code EmailSender}/{@code SmsSender}/{@code PushSender}
 * directly on the request thread should call one of these methods instead —
 * see {@code AuthService}, {@code StaffService}, {@code BookingService}, and
 * {@code NotificationService} for the first callers. Anything enqueued here is
 * only actually sent by {@code OutboxDispatcher}'s background poller.
 *
 * <p>Callers running inside an {@code @Transactional("mongoTransactionManager")}
 * method (e.g. {@code BookingService#cancel}) get the outbox row committed
 * atomically with the business change for free, since this just issues another
 * {@code MongoTemplate} write against the same session/transaction. Callers that
 * are NOT transactional (e.g. {@code AuthService#signup}, which is deliberately
 * left unannotated so it still works against a non-replica-set dev Mongo — see
 * its class javadoc) get the same non-atomicity they already had with a direct
 * {@code EmailSender.send()} call — this is a strict improvement (the send is no
 * longer inline/blocking/unretried), never a regression.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxMessageRepository repository;
    private final OutboxProperties properties;

    public void enqueueEmail(String shopId, String toEmail, String subject, String htmlBody, String eventType, String referenceId) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        enqueue(shopId, OutboxChannel.EMAIL, toEmail, subject, htmlBody, eventType, referenceId);
    }

    public void enqueueSms(String shopId, String toPhoneE164, String body, String eventType, String referenceId) {
        if (toPhoneE164 == null || toPhoneE164.isBlank()) {
            return;
        }
        enqueue(shopId, OutboxChannel.SMS, toPhoneE164, null, body, eventType, referenceId);
    }

    public void enqueuePush(String shopId, String deviceToken, String title, String body, String eventType, String referenceId) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return;
        }
        enqueue(shopId, OutboxChannel.PUSH, deviceToken, title, body, eventType, referenceId);
    }

    private void enqueue(String shopId, OutboxChannel channel, String recipient, String subject, String body,
                         String eventType, String referenceId) {
        OutboxMessage message = new OutboxMessage();
        message.setShopId(shopId);
        message.setChannel(channel);
        message.setRecipient(recipient);
        message.setSubject(subject);
        message.setBody(body);
        message.setEventType(eventType);
        message.setReferenceId(referenceId);
        message.setMaxAttempts(properties.getMaxAttempts());
        repository.insert(message);
    }
}
