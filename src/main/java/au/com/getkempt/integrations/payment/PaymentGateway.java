package au.com.getkempt.integrations.payment;

import au.com.getkempt.common.money.Money;

import java.util.Map;

/**
 * Port for the payments provider (Stripe Connect in production). Domain code depends
 * only on this interface, never on the Stripe SDK, so payment logic stays testable
 * and vendor-agnostic. A local stub implements it until the real adapter lands.
 */
public interface PaymentGateway {

    record CreateIntent(Money amount, String purpose, boolean manualCapture, Map<String, String> metadata) {
    }

    record IntentResult(String intentId, String clientSecret, String status) {
    }

    record RefundResult(String refundId, long refundedCents, String status) {
    }

    IntentResult createIntent(CreateIntent command);

    void capture(String intentId);

    void cancel(String intentId);

    RefundResult refund(String chargeId, Money amount, String reason);
}
