package au.com.getkempt.request.retail;

import au.com.getkempt.common.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create/update body for a {@code Product}. POST validates required fields (via
 * {@code @Valid} at the controller); PATCH reuses the same type without
 * {@code @Valid} and applies non-null fields only — same convention as
 * {@code ServiceWriteRequest}.
 */
public record ProductWriteRequest(
        @NotBlank String name,
        String sku,
        String description,
        String category,
        @NotNull @Valid Money price,
        @Valid Money cost,
        Boolean trackInventory,
        @Min(0) Integer stockQuantity,
        @Min(0) Integer lowStockThreshold,
        Boolean active) {
}
