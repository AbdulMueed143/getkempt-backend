package au.com.getkempt.responses.staff;

import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the contract's {@code StaffInvitation}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffInvitationResponse(
        String id,
        String email,
        StaffRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt) {
}
