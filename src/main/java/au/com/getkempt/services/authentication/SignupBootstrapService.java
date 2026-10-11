package au.com.getkempt.services.authentication;

import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.common.tenant.TenantContext;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.entities.User;
import au.com.getkempt.enums.UserRole;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import au.com.getkempt.enums.staff.CommissionType;
import au.com.getkempt.enums.staff.StaffStatus;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.repositories.service.ServiceRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.services.staff.StaffServiceLinkSync;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Provisions the owner {@link StaffMember} and a default catalogue service when a shop
 * signs up. Runs under a temporary tenant context because signup happens before a JWT
 * exists.
 */
@Service
@RequiredArgsConstructor
public class SignupBootstrapService {

    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final StaffServiceLinkSync linkSync;

    public StaffMember bootstrap(User user, UserRole role) {
        TenantContextHolder.set(TenantContext.builder()
                .shopId(user.getShopId())
                .userId(user.getId())
                .role(StaffRole.OWNER.wire())
                .build());
        try {
            StaffMember owner = new StaffMember();
            owner.setUserId(user.getId());
            owner.setFirstName(user.getFirstName());
            owner.setLastName(user.getLastName());
            owner.setEmail(user.getEmail());
            owner.setPhone("");
            owner.setRole(StaffRole.OWNER);
            owner.setStatus(StaffStatus.ACTIVE);
            owner.setCommissionType(CommissionType.COMMISSION);
            owner.setStartDate(LocalDate.now());
            owner.setAvatarColor("#1B3163");
            owner.setCalendarColor("#4A90D9");
            staffRepository.insert(owner);

            au.com.getkempt.entities.Service defaultService = new au.com.getkempt.entities.Service();
            defaultService.setName(role == UserRole.BARBER ? "Haircut & Beard" : "Haircut");
            defaultService.setCategory(role == UserRole.BARBER
                    ? ServiceCategory.BARBER
                    : ServiceCategory.HAIR);
            defaultService.setDescription("");
            defaultService.setDurationMinutes(30);
            defaultService.setBufferMinutes(0);
            defaultService.setPrice(Money.ofAud(6500));
            defaultService.setDepositRequired(false);
            defaultService.setStatus(ServiceStatus.ACTIVE);
            defaultService.setOnlineBookingEnabled(true);
            defaultService.setDisplayOrder(0);
            serviceRepository.insert(defaultService);

            linkSync.replaceServiceStaff(defaultService.getId(), List.of(owner.getId()));
            return owner;
        } finally {
            TenantContextHolder.clear();
        }
    }
}
