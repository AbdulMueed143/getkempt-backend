package au.com.getkempt.services.pkg;

import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.responses.pkg.PackagePricePreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Derives package pricing and duration from the referenced services. Values are never
 * trusted from the client — they are computed at read/preview time.
 */
@Service
@RequiredArgsConstructor
public class PackagePricingService {

    public record Pricing(Money basePrice, Money discountedPrice, int effectiveDurationMinutes) {
    }

    public Pricing compute(List<au.com.getkempt.entities.Service> services,
                           Integer customDurationMinutes,
                           boolean discountEnabled,
                           DiscountType discountType,
                           Double discountValue) {
        if (services == null || services.size() < 2) {
            throw new UnprocessableEntityException("A package must contain at least 2 services");
        }

        Money basePrice = Money.zero();
        int duration = 0;
        for (au.com.getkempt.entities.Service service : services) {
            basePrice = basePrice.plus(service.getPrice());
            duration += service.getDurationMinutes() + service.getBufferMinutes();
        }

        int effectiveDuration = customDurationMinutes != null ? customDurationMinutes : duration;
        Money discountedPrice = applyDiscount(basePrice, discountEnabled, discountType, discountValue);
        return new Pricing(basePrice, discountedPrice, effectiveDuration);
    }

    public PackagePricePreviewResponse toPreview(Pricing pricing) {
        Money discountAmount = pricing.basePrice().minus(pricing.discountedPrice());
        return new PackagePricePreviewResponse(
                pricing.basePrice(),
                pricing.discountedPrice(),
                discountAmount,
                pricing.effectiveDurationMinutes());
    }

    private Money applyDiscount(Money base,
                                boolean discountEnabled,
                                DiscountType discountType,
                                Double discountValue) {
        if (!discountEnabled || discountType == null || discountValue == null) {
            return base;
        }
        return switch (discountType) {
            case PERCENTAGE -> {
                if (discountValue < 0 || discountValue > 100) {
                    throw new UnprocessableEntityException("Discount percentage must be between 0 and 100");
                }
                long discountCents = Math.round(base.amountCents() * (discountValue / 100.0));
                yield new Money(Math.max(0, base.amountCents() - discountCents), base.currency());
            }
            case FIXED -> {
                if (discountValue < 0) {
                    throw new UnprocessableEntityException("Fixed discount must not be negative");
                }
                long discountCents = Math.round(discountValue * 100);
                yield new Money(Math.max(0, base.amountCents() - discountCents), base.currency());
            }
        };
    }
}
