package au.com.getkempt.responses.staff;

import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.CommissionType;
import au.com.getkempt.enums.staff.StaffStatus;
import au.com.getkempt.responses.ImageRefResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Wire shape of the contract's {@code StaffMember}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffResponse(
        String id,
        String userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String bio,
        String specialization,
        ImageRefResponse avatarImage,
        String avatarColor,
        String calendarColor,
        StaffRole role,
        StaffStatus status,
        List<String> services,
        CommissionType commissionType,
        Double commissionRate,
        LocalDate startDate,
        Instant createdAt,
        Instant deletedAt) {
}
