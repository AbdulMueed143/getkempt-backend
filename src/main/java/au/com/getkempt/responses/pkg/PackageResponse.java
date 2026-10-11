package au.com.getkempt.responses.pkg;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.enums.pkg.PackageStatus;
import au.com.getkempt.responses.ImageRefResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code Package}, including the server-derived pricing. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PackageResponse(
        String id,
        String name,
        String description,
        ImageRefResponse image,
        List<String> serviceIds,
        Integer customDurationMinutes,
        boolean discountEnabled,
        DiscountType discountType,
        Double discountValue,
        PackageStatus status,
        boolean onlineBookingEnabled,
        List<String> staffIds,
        Money basePrice,
        Money discountedPrice,
        int effectiveDurationMinutes,
        Instant createdAt,
        Instant updatedAt) {
}
