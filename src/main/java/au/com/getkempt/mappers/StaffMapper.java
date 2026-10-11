package au.com.getkempt.mappers;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.entities.StaffInvitation;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.responses.ImageRefResponse;
import au.com.getkempt.responses.staff.StaffInvitationResponse;
import au.com.getkempt.responses.staff.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StaffMapper {

    private final FileStorage fileStorage;

    public StaffResponse toResponse(StaffMember staff) {
        return new StaffResponse(
                staff.getId(),
                staff.getUserId(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getPhone(),
                staff.getBio(),
                staff.getSpecialization(),
                toImageRef(staff.getAvatarImage()),
                staff.getAvatarColor(),
                staff.getCalendarColor(),
                staff.getRole(),
                staff.getStatus(),
                nullSafe(staff.getServices()),
                staff.getCommissionType(),
                staff.getCommissionRate(),
                staff.getStartDate(),
                staff.getCreatedAt(),
                staff.getDeletedAt());
    }

    public StaffInvitationResponse toResponse(StaffInvitation invitation) {
        return new StaffInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt());
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
