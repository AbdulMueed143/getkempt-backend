package au.com.getkempt.responses.retail;

import au.com.getkempt.responses.payment.PaymentIntentResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape for {@code POST /sales}'s 201 response — mirrors {@code BookingCreateResponse}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SaleCreateResponse(SaleResponse sale, PaymentIntentResponse paymentIntent) {
}
