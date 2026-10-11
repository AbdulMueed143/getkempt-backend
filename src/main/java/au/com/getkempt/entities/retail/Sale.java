package au.com.getkempt.entities.retail;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.payment.PaymentChannel;
import au.com.getkempt.enums.retail.SaleStatus;
import au.com.getkempt.enums.store.InStoreMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A completed (or in-flight) POS checkout of one or more {@link Product} line
 * items — the "basic mechanism" for shops that sell retail product, not just
 * services. Deliberately minimal for v1: no tax/discount line, no split
 * payments, no partial refunds (a full {@code voidSale} is as far as the
 * lifecycle goes) — see {@code SaleService} javadoc for what's left for the
 * eventual Payments/Charges domain to pick up.
 *
 * <p>Mirrors {@code Booking}'s online-vs-in-store payment shape exactly:
 * {@link #paymentChannel} ONLINE means a {@link #paymentIntentId} was created
 * against the shop's connected Stripe account (same {@code PaymentGateway}
 * stub Bookings uses); IN_STORE means {@link #inStoreMethod} records which of
 * the shop's {@code StoreSettings.inStoreMethods} was actually used.
 */
@Document(collection = "sales")
@Getter
@Setter
public class Sale extends BaseDocument {

    private String clientId;
    private String clientName;

    @Indexed
    private String staffId;

    /** Optional link when items were rung up alongside a completed appointment. */
    private String bookingId;

    private List<SaleLineItem> lineItems = new ArrayList<>();

    private Money subtotal;
    private Money total;

    private PaymentChannel paymentChannel;
    private InStoreMethod inStoreMethod;
    private String paymentIntentId;

    private SaleStatus status;
    private Instant soldAt;
    private String voidReason;
}
