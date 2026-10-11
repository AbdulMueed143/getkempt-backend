package au.com.getkempt.request.retail;

import jakarta.validation.constraints.NotNull;

/** @param delta Positive to restock, negative for shrinkage/breakage/correction. */
public record StockAdjustRequest(@NotNull Integer delta, String reason) {
}
