package au.com.getkempt.mappers;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.entities.Service;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.responses.ImageRefResponse;
import au.com.getkempt.responses.service.ServiceCategoryResponse;
import au.com.getkempt.responses.service.ServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceMapper {

    private final FileStorage fileStorage;

    public ServiceResponse toResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getCategory(),
                service.getDescription(),
                toImageRef(service.getImage()),
                service.getDurationMinutes(),
                service.getBufferMinutes(),
                service.getPrice(),
                service.isDepositRequired(),
                service.getDepositAmount(),
                service.getStatus(),
                service.isOnlineBookingEnabled(),
                nullSafe(service.getStaffIds()),
                service.getDisplayOrder(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }

    public ImageRefResponse toImageRef(StoredImageRef ref) {
        if (ref == null || ref.getKey() == null) {
            return null;
        }
        return new ImageRefResponse(ref.getKey(), fileStorage.publicUrl(ref.getKey()), ref.getWidth(), ref.getHeight());
    }

    public List<ServiceCategoryResponse> categoryCatalog() {
        return List.of(
                category(ServiceCategory.HAIR, "Hair", "#7C3AED", "#EDE9FE"),
                category(ServiceCategory.BARBER, "Barber", "#2563EB", "#DBEAFE"),
                category(ServiceCategory.NAILS, "Nails", "#DB2777", "#FCE7F3"),
                category(ServiceCategory.BEAUTY, "Beauty", "#EA580C", "#FFEDD5"),
                category(ServiceCategory.LASH_BROW, "Lash & Brow", "#9333EA", "#F3E8FF"),
                category(ServiceCategory.MASSAGE, "Massage", "#059669", "#D1FAE5"),
                category(ServiceCategory.OTHER, "Other", "#64748B", "#F1F5F9"));
    }

    private ServiceCategoryResponse category(ServiceCategory value, String label, String color, String bg) {
        return new ServiceCategoryResponse(value, label, color, bg);
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
