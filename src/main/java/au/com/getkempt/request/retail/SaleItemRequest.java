package au.com.getkempt.request.retail;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(@NotBlank String productId, @NotNull @Min(1) Integer quantity) {
}
