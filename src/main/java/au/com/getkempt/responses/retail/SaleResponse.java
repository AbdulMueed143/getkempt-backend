package au.com.getkempt.responses.retail;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.payment.PaymentChannel;
import au.com.getkempt.enums.retail.SaleStatus;
import au.com.getkempt.enums.store.InStoreMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SaleResponse(
        String id,
        String clientId,
        String clientName,
        String staffId,
        String bookingId,
        List<SaleLineItemResponse> lineItems,
        Money subtotal,
        Money total,
        PaymentChannel paymentChannel,
        InStoreMethod inStoreMethod,
        String paymentIntentId,
        SaleStatus status,
        Instant soldAt,
        String voidReason,
        Instant createdAt,
        Long version) {
}
