package au.com.getkempt.responses.payment;

import au.com.getkempt.common.money.Money;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Wire shape of the contract's {@code PaymentIntent}. Defined ahead of the
 * Payments domain itself (not yet implemented) purely so {@code POST /bookings}
 * can return a contract-correct {@code paymentIntent} object today, backed by
 * {@code PaymentGateway}'s stub adapter — the real Payments domain will reuse
 * this exact type with no wire-format change once built.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentIntentResponse(
        String id,
        String bookingId,
        String clientId,
        Money amount,
        String captureMethod,
        String status,
        String clientSecret,
        Instant createdAt) {
}
