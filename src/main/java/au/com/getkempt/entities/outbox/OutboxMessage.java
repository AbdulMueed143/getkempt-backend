package au.com.getkempt.entities.outbox;

import au.com.getkempt.enums.outbox.OutboxChannel;
import au.com.getkempt.enums.outbox.OutboxStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Transactional-outbox row: one queued email/SMS/push send.
 *
 * <p>This is the whole point of the pattern — the row is written by the SAME
 * method (and, where that method is {@code @Transactional("mongoTransactionManager")},
 * the SAME database transaction) that performs the triggering business change
 * (signup, a booking confirmation, a cancellation, ...). The actual network call
 * to the email/SMS/push provider never happens inline on the request thread; a
 * separate {@code OutboxDispatcher} poller drains {@code PENDING} rows on its own
 * schedule. Two things fall out of that for free:
 *
 * <ul>
 *   <li>A slow or down provider (Twilio outage, SendGrid latency spike) can never
 *       slow down or fail a signup/booking API call — the business write already
 *       committed before anything provider-related is attempted.</li>
 *   <li>Sends survive a provider failure: the row stays {@code PENDING}/{@code FAILED}
 *       and is retried with backoff rather than being silently lost because an
 *       exception unwound the request.</li>
 * </ul>
 *
 * <p>Deliberately NOT a {@code TenantRepository}-backed aggregate: the dispatcher
 * is a background process with no request/JWT/tenant context, and by design must
 * scan PENDING rows across every shop in one pass, so it's queried directly via
 * {@code OutboxMessageRepository} (a plain {@code MongoTemplate} wrapper) instead.
 * {@code shopId} is still stamped on every row for observability/debugging.
 */
@Document(collection = "outbox_messages")
@Getter
@Setter
public class OutboxMessage {

    @Id
    private String id;

    @Indexed
    private String shopId;

    private OutboxChannel channel;

    @Indexed
    private OutboxStatus status;

    /** Email address / E.164 phone number / push device token, depending on {@link #channel}. */
    private String recipient;

    /** Email subject or push title. Unused for SMS. */
    private String subject;

    private String body;

    /** Debugging/observability tag, e.g. {@code "auth.verify_email"} or {@code "booking.cancelled"} — never parsed. */
    private String eventType;

    /** Correlates back to the triggering aggregate (userId, bookingId, saleId, ...). Optional. */
    private String referenceId;

    private int attempts;
    private int maxAttempts;

    @Indexed
    private Instant nextAttemptAt;

    private String lastError;
    private String providerMessageId;

    private Instant createdAt;
    private Instant claimedAt;
    private Instant sentAt;
}
