package au.com.getkempt.request.pkg;

import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.enums.pkg.PackageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create/update body for a package. POST validates required fields; PATCH reuses the
 * type without {@code @Valid} and applies non-null fields only. Pricing is never
 * accepted from the client — it is derived server-side from the referenced services.
 */
public record PackageWriteRequest(
        @NotBlank String name,
        String description,
        @NotNull @Size(min = 2, message = "A package must contain at least 2 services") List<String> serviceIds,
        Integer customDurationMinutes,
        Boolean discountEnabled,
        DiscountType discountType,
        Double discountValue,
        PackageStatus status,
        Boolean onlineBookingEnabled,
        @NotNull List<String> staffIds) {
}
