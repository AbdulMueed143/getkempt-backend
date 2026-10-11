package au.com.getkempt.request.booking;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.store.InStoreMethod;

import java.time.Instant;

/**
 * @param inStoreMethod How payment was actually taken in person, when the booking
 *                       has no online PaymentIntent. Validated against the shop's
 *                       {@code StoreSettings.inStoreMethods} allow-list; optional
 *                       and non-breaking for callers that don't record it.
 */
public record CompleteBookingRequest(Instant actualEndAt, Money tip, InStoreMethod inStoreMethod) {
}
