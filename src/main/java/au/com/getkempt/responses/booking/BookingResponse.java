package au.com.getkempt.responses.booking;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.booking.BookingSource;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.enums.booking.CancellationReason;
import au.com.getkempt.enums.store.InStoreMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code Booking}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(
        String id,
        String clientId,
        String clientName,
        String clientPhone,
        String clientEmail,
        String staffId,
        String staffName,
        String serviceId,
        String packageId,
        String serviceName,
        int durationMinutes,
        Money price,
        Instant startAt,
        Instant endAt,
        BookingStatus status,
        BookingSource source,
        String notes,
        CancellationReason cancellationReason,
        String paymentIntentId,
        Money depositPaid,
        InStoreMethod inStoreMethodUsed,
        List<AppliedSurchargeResponse> surchargesApplied,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
