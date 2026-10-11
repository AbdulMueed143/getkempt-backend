package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.BookingWindowUnit;
import au.com.getkempt.enums.store.DepositType;
import au.com.getkempt.enums.store.FeeType;
import au.com.getkempt.enums.store.InStoreMethod;
import au.com.getkempt.enums.store.PaymentMode;

import java.util.List;

/**
 * {@code PATCH /store/settings} body — all fields optional (null == "leave unchanged").
 * Embedded lists (holidays / surcharge / cancellation rules) are managed through their
 * dedicated sub-resource endpoints, not here.
 */
public record StoreSettingsUpdateRequest(
        Integer bookingWindowValue,
        BookingWindowUnit bookingWindowUnit,
        Integer slotInterval,
        PaymentMode paymentMode,
        DepositType depositType,
        Double depositValue,
        List<InStoreMethod> inStoreMethods,
        FeeType noShowFeeType,
        Double noShowFeeValue,
        Boolean autoCharge,
        String cancellationPolicy,
        String privacyPolicy) {
}
