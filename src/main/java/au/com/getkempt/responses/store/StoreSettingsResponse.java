package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.BookingWindowUnit;
import au.com.getkempt.enums.store.DepositType;
import au.com.getkempt.enums.store.FeeType;
import au.com.getkempt.enums.store.InStoreMethod;
import au.com.getkempt.enums.store.PaymentMode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code StoreSettings}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoreSettingsResponse(
        int bookingWindowValue,
        BookingWindowUnit bookingWindowUnit,
        int slotInterval,
        List<HolidayResponse> holidays,
        List<SurchargeRuleResponse> surchargeRules,
        PaymentMode paymentMode,
        DepositType depositType,
        Double depositValue,
        List<InStoreMethod> inStoreMethods,
        boolean stripeConnected,
        FeeType noShowFeeType,
        Double noShowFeeValue,
        boolean autoCharge,
        List<CancellationFeeRuleResponse> cancellationFeeRules,
        String cancellationPolicy,
        String privacyPolicy,
        Instant updatedAt) {
}
