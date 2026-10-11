package au.com.getkempt.entities;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.CommissionType;
import au.com.getkempt.enums.staff.StaffStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A member of the shop. May be linked to a login {@link User} via {@code userId}
 * (the owner always is). {@code services} holds the ids of services this member can
 * perform — the inverse of {@code Service.staffIds}, kept in sync by the services.
 */
@Document(collection = "staff_members")
@Getter
@Setter
public class StaffMember extends BaseDocument {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private String specialization;
    private StoredImageRef avatarImage;
    private String avatarColor;
    private String calendarColor;
    private StaffRole role;
    private StaffStatus status;

    private List<String> services = new ArrayList<>();

    private CommissionType commissionType;
    private Double commissionRate;
    private LocalDate startDate;
}
