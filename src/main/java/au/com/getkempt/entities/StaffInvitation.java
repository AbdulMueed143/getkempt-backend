package au.com.getkempt.entities;

import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.InvitationStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** An outstanding invite for someone to join the shop with a given role. */
@Document(collection = "staff_invitations")
@Getter
@Setter
public class StaffInvitation extends BaseDocument {

    private String email;
    private StaffRole role;
    private InvitationStatus status;

    /** Optionally links the invite to an already-created staff record. */
    private String linkedStaffId;

    /** SHA-256 of the opaque acceptance token e-mailed to the invitee. */
    private String tokenHash;

    private Instant expiresAt;
    private Instant acceptedAt;
}
