package au.com.getkempt.services.payment;

import au.com.getkempt.common.error.exceptions.PaymentRequiredException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.enums.store.InStoreMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single place that enforces "the shop chooses which payment methods are
 * allowed" — shared by {@code BookingService} (completing/paying for an
 * appointment) and {@code SaleService} (retail POS checkout) so the rule can't
 * drift between the two call sites.
 */
@Component
public class PaymentMethodPolicy {

    /** Throws 422 if {@code method} is not in the shop's {@code StoreSettings.inStoreMethods} allow-list. */
    public void requireAllowed(StoreSettings settings, InStoreMethod method) {
        List<InStoreMethod> allowed = settings.getInStoreMethods();
        if (allowed == null || !allowed.contains(method)) {
            throw new UnprocessableEntityException(
                    "This shop does not accept " + method.value() + " as an in-store payment method");
        }
    }

    /** Throws 402 if the shop hasn't completed Stripe Connect onboarding yet, so online payment can't actually settle. */
    public void requireOnlineCapable(StoreSettings settings) {
        if (!settings.isStripeConnected()) {
            throw new PaymentRequiredException(
                    "This shop has not connected Stripe yet — online payment is unavailable until onboarding completes");
        }
    }
}
