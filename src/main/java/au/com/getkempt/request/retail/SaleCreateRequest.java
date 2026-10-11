package au.com.getkempt.request.retail;

import au.com.getkempt.enums.payment.PaymentChannel;
import au.com.getkempt.enums.store.InStoreMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @param inStoreMethod Required when {@code paymentChannel=in_store}; validated against
 *                       the shop's {@code StoreSettings.inStoreMethods} allow-list. Ignored
 *                       (must be omitted) when {@code paymentChannel=online}.
 */
public record SaleCreateRequest(
        String clientId,
        String bookingId,
        @NotEmpty @Valid List<SaleItemRequest> items,
        @NotNull PaymentChannel paymentChannel,
        InStoreMethod inStoreMethod) {
}
