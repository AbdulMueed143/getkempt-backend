package au.com.getkempt.request.staff;

import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.CommissionType;
import au.com.getkempt.enums.staff.StaffStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Create/update body for a staff member. POST validates the required fields (via
 * {@code @Valid}); PATCH reuses the type without {@code @Valid} and applies only the
 * non-null fields, so it behaves as a partial update.
 */
public record StaffWriteRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        String bio,
        String specialization,
        @NotNull StaffRole role,
        StaffStatus status,
        @NotNull List<String> services,
        @NotNull CommissionType commissionType,
        Double commissionRate,
        @NotNull LocalDate startDate,
        String avatarColor,
        String calendarColor,
        Boolean sendInvite) {
}
