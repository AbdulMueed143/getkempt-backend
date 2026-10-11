package au.com.getkempt.request.staff;

import au.com.getkempt.common.security.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
        @NotBlank @Email String email,
        @NotNull StaffRole role,
        String staffId) {
}
