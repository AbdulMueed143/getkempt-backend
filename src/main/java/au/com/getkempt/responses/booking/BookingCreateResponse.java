package au.com.getkempt.responses.booking;

import au.com.getkempt.responses.payment.PaymentIntentResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape for {@code POST /bookings}'s 201 response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingCreateResponse(BookingResponse booking, PaymentIntentResponse paymentIntent) {
}
