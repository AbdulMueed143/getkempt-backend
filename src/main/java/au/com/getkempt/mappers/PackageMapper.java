package au.com.getkempt.mappers;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.entities.Package;
import au.com.getkempt.entities.Service;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.responses.ImageRefResponse;
import au.com.getkempt.responses.pkg.PackageResponse;
import au.com.getkempt.services.pkg.PackagePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PackageMapper {

    private final FileStorage fileStorage;
    private final PackagePricingService pricingService;

    public PackageResponse toResponse(Package pkg, List<Service> services) {
        PackagePricingService.Pricing pricing = pricingService.compute(
                services,
                pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(),
                pkg.getDiscountType(),
                pkg.getDiscountValue());

        return new PackageResponse(
                pkg.getId(),
                pkg.getName(),
                pkg.getDescription(),
                toImageRef(pkg.getImage()),
                nullSafe(pkg.getServiceIds()),
                pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(),
                pkg.getDiscountType(),
                pkg.getDiscountValue(),
                pkg.getStatus(),
                pkg.isOnlineBookingEnabled(),
                nullSafe(pkg.getStaffIds()),
                pricing.basePrice(),
                pricing.discountedPrice(),
                pricing.effectiveDurationMinutes(),
                pkg.getCreatedAt(),
                pkg.getUpdatedAt());
    }

    public ImageRefResponse toImageRef(StoredImageRef ref) {
        if (ref == null || ref.getKey() == null) {
            return null;
        }
        return new ImageRefResponse(ref.getKey(), fileStorage.publicUrl(ref.getKey()), ref.getWidth(), ref.getHeight());
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
