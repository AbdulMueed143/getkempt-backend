package au.com.getkempt.responses.pkg;

import au.com.getkempt.common.money.Money;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of {@code GET /packages/{id}/preview-price}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PackagePricePreviewResponse(
        Money basePrice,
        Money discountedPrice,
        Money discountAmount,
        int effectiveDurationMinutes) {
}
