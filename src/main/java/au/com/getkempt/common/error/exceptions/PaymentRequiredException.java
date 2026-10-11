package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 402 — Stripe Connect missing, insufficient SMS credits, plan quota exceeded,
 * or a fee owed. See {@code docs/01-conventions.md §9} and {@code 08-rate-limiting.md}.
 */
public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, "payment_required", message);
    }
}
