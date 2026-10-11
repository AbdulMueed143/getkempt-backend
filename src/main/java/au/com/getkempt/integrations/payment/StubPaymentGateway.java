package au.com.getkempt.integrations.payment;

import au.com.getkempt.common.money.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Local no-op payment gateway: mints fake Stripe-shaped ids and logs. A real
 * adapter will later replace it (e.g. via {@code @Primary} or a Spring profile).
 */
@Component
@Slf4j
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public IntentResult createIntent(CreateIntent command) {
        String intentId = "pi_stub_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[stub-payment] createIntent purpose={} amount={} manualCapture={} -> {}",
                command.purpose(), command.amount(), command.manualCapture(), intentId);
        return new IntentResult(intentId, intentId + "_secret", "requires_payment_method");
    }

    @Override
    public void capture(String intentId) {
        log.info("[stub-payment] capture {}", intentId);
    }

    @Override
    public void cancel(String intentId) {
        log.info("[stub-payment] cancel {}", intentId);
    }

    @Override
    public RefundResult refund(String chargeId, Money amount, String reason) {
        String refundId = "re_stub_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[stub-payment] refund charge={} amount={} reason={} -> {}", chargeId, amount, reason, refundId);
        return new RefundResult(refundId, amount == null ? 0 : amount.amountCents(), "succeeded");
    }
}
