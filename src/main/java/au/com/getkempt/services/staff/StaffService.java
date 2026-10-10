package au.com.getkempt.services.staff;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.pagination.PageInfo;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.common.security.Tokens;
import au.com.getkempt.common.tenant.TenantContext;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.entities.StaffInvitation;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.enums.staff.CommissionType;
import au.com.getkempt.enums.staff.InvitationStatus;
import au.com.getkempt.enums.staff.StaffStatus;
import au.com.getkempt.mappers.StaffMapper;
import au.com.getkempt.repositories.staff.StaffInvitationRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.request.staff.CreateInvitationRequest;
import au.com.getkempt.request.staff.StaffWriteRequest;
import au.com.getkempt.responses.staff.StaffInvitationResponse;
import au.com.getkempt.responses.staff.StaffResponse;
import au.com.getkempt.services.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Staff administration: CRUD, service assignments, avatars, activation, and invitations.
 * Permission rules follow the contract — owners have full access; managers cannot touch
 * pay fields or the owner; staff can only read their own record.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "lastName", "startDate");
    private static final long INVITE_TTL_DAYS = 7;

    private final StaffRepository staffRepository;
    private final StaffInvitationRepository invitationRepository;
    private final StaffServiceLinkSync linkSync;
    private final StaffMapper staffMapper;
    private final OutboxService outboxService;

    public CursorPage<StaffResponse> list(String cursor,
                                          Integer limit,
                                          String sortParam,
                                          StaffStatus status,
                                          StaffRole role,
                                          String q) {
        TenantContext ctx = TenantContextHolder.require();
        requireReadAccess(ctx);

        if (isStaffRole(ctx) && ctx.staffId() != null) {
            StaffMember self = staffRepository.requireById(ctx.staffId());
            return new CursorPage<>(List.of(staffMapper.toResponse(self)), PageInfo.of(null, false));
        }

        Criteria filter = new Criteria();
        if (status != null) {
            filter.and("status").is(status);
        }
        if (role != null) {
            filter.and("role").is(role);
        }
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE);
            filter.orOperator(
                    Criteria.where("firstName").regex(pattern),
                    Criteria.where("lastName").regex(pattern),
                    Criteria.where("email").regex(pattern));
        }

        SortRequest sort = SortRequest.parse(sortParam, SORTABLE, SortRequest.asc("lastName"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return staffRepository.list(filter, sort, page).map(staffMapper::toResponse);
    }

    public StaffResponse get(String staffId) {
        TenantContext ctx = TenantContextHolder.require();
        StaffMember staff = staffRepository.requireById(staffId);
        requireCanView(ctx, staff);
        return staffMapper.toResponse(staff);
    }

    public StaffResponse create(StaffWriteRequest request) {
        requireManageAccess();
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);

        StaffMember staff = new StaffMember();
        applyWrite(staff, request, true);
        staff.setEmail(email);
        staff.setStatus(request.status() != null ? request.status() : StaffStatus.ACTIVE);
        if (staff.getAvatarColor() == null) {
            staff.setAvatarColor("#1B3163");
        }
        if (staff.getCalendarColor() == null) {
            staff.setCalendarColor("#4A90D9");
        }

        staffRepository.insert(staff);
        linkSync.replaceStaffServices(staff.getId(), nullSafe(request.services()));

        if (Boolean.TRUE.equals(request.sendInvite())) {
            sendInvitation(email, request.role(), staff.getId());
        }
        return staffMapper.toResponse(staffRepository.requireById(staff.getId()));
    }

    public StaffResponse update(String staffId, String ifMatch, StaffWriteRequest request) {
        TenantContext ctx = TenantContextHolder.require();
        requireManageAccess();

        StaffMember staff = staffRepository.requireById(staffId);
        IfMatchSupport.check(ifMatch, staff.getVersion());
        assertManagerCanModify(ctx, staff);

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            ensureEmailAvailable(email, staffId);
            staff.setEmail(email);
        }
        applyWrite(staff, request, false);

        staffRepository.save(staff);
        if (request.services() != null) {
            linkSync.replaceStaffServices(staffId, request.services());
        }
        return staffMapper.toResponse(staffRepository.requireById(staffId));
    }

    public void delete(String staffId) {
        requireManageAccess();
        StaffMember staff = staffRepository.requireById(staffId);
        if (staff.getRole() == StaffRole.OWNER) {
            throw new ForbiddenException("The shop owner cannot be removed");
        }
        staffRepository.softDelete(staff);
    }

    public StaffResponse setServices(String staffId, List<String> serviceIds) {
        requireManageAccess();
        staffRepository.requireById(staffId);
        linkSync.replaceStaffServices(staffId, serviceIds);
        return staffMapper.toResponse(staffRepository.requireById(staffId));
    }

    public StaffResponse setAvatar(String staffId, String uploadId) {
        requireManageAccess();
        StaffMember staff = staffRepository.requireById(staffId);
        staff.setAvatarImage(StoredImageRef.builder().key("staff/" + staffId + "/avatar/" + uploadId).build());
        staffRepository.save(staff);
        return staffMapper.toResponse(staff);
    }

    public void deleteAvatar(String staffId) {
        requireManageAccess();
        StaffMember staff = staffRepository.requireById(staffId);
        staff.setAvatarImage(null);
        staffRepository.save(staff);
    }

    public StaffResponse deactivate(String staffId) {
        requireManageAccess();
        StaffMember staff = staffRepository.requireById(staffId);
        if (staff.getRole() == StaffRole.OWNER) {
            throw new ForbiddenException("The shop owner cannot be deactivated");
        }
        staff.setStatus(StaffStatus.INACTIVE);
        staffRepository.save(staff);
        return staffMapper.toResponse(staff);
    }

    public StaffResponse reactivate(String staffId) {
        requireManageAccess();
        StaffMember staff = staffRepository.requireById(staffId);
        staff.setStatus(StaffStatus.ACTIVE);
        staffRepository.save(staff);
        return staffMapper.toResponse(staff);
    }

    public List<StaffInvitationResponse> listInvitations() {
        requireManageAccess();
        return invitationRepository.findAllActive().stream().map(staffMapper::toResponse).toList();
    }

    public StaffInvitationResponse createInvitation(CreateInvitationRequest request) {
        requireManageAccess();
        String email = normalizeEmail(request.email());
        if (staffRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("A staff member with this email already exists");
        }
        invitationRepository.findPendingByEmail(email).ifPresent(existing -> {
            throw new ConflictException("An invitation for this email is already pending");
        });

        if (request.staffId() != null) {
            staffRepository.requireById(request.staffId());
        }
        return staffMapper.toResponse(sendInvitation(email, request.role(), request.staffId()));
    }

    public void revokeInvitation(String invitationId) {
        requireManageAccess();
        StaffInvitation invitation = invitationRepository.requireById(invitationId);
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    public StaffInvitationResponse resendInvitation(String invitationId) {
        requireManageAccess();
        StaffInvitation invitation = invitationRepository.requireById(invitationId);
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new UnprocessableEntityException("Only pending invitations can be resent");
        }
        String rawToken = Tokens.randomToken();
        invitation.setTokenHash(Tokens.sha256(rawToken));
        invitation.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
        invitationRepository.save(invitation);
        sendInviteEmail(invitation.getEmail(), rawToken);
        return staffMapper.toResponse(invitation);
    }

    // --- helpers ---------------------------------------------------------------

    private StaffInvitation sendInvitation(String email, StaffRole role, String linkedStaffId) {
        String rawToken = Tokens.randomToken();
        StaffInvitation invitation = new StaffInvitation();
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setLinkedStaffId(linkedStaffId);
        invitation.setTokenHash(Tokens.sha256(rawToken));
        invitation.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
        invitationRepository.insert(invitation);
        sendInviteEmail(email, rawToken);
        return invitation;
    }

    /** Queued via {@link OutboxService} — see its class javadoc; same pattern as {@code AuthService}. */
    private void sendInviteEmail(String email, String rawToken) {
        String link = "https://app.getkempt.co/accept-invite?token=" + rawToken;
        outboxService.enqueueEmail(TenantContextHolder.requireShopId(), email, "You're invited to join GetKempt",
                "<p>You've been invited to join a shop on GetKempt. "
                        + "<a href=\"" + link + "\">Accept invitation</a></p>",
                "staff.invitation", null);
    }

    private void applyWrite(StaffMember staff, StaffWriteRequest request, boolean creating) {
        TenantContext ctx = TenantContextHolder.require();

        if (request.firstName() != null) {
            staff.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            staff.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            staff.setPhone(request.phone());
        }
        if (request.bio() != null) {
            staff.setBio(request.bio());
        }
        if (request.specialization() != null) {
            staff.setSpecialization(request.specialization());
        }
        if (request.role() != null) {
            if (!creating && staff.getRole() == StaffRole.OWNER) {
                throw new ForbiddenException("The shop owner's role cannot be changed");
            }
            if (isManager(ctx) && request.role() == StaffRole.OWNER) {
                throw new ForbiddenException("Managers cannot assign the owner role");
            }
            staff.setRole(request.role());
        }
        if (request.status() != null) {
            staff.setStatus(request.status());
        }
        if (request.commissionType() != null) {
            assertCanEditPay(ctx);
            staff.setCommissionType(request.commissionType());
        } else if (creating && staff.getCommissionType() == null) {
            staff.setCommissionType(CommissionType.COMMISSION);
        }
        if (request.commissionRate() != null) {
            assertCanEditPay(ctx);
            staff.setCommissionRate(request.commissionRate());
        }
        if (request.startDate() != null) {
            staff.setStartDate(request.startDate());
        }
        if (request.avatarColor() != null) {
            staff.setAvatarColor(request.avatarColor());
        }
        if (request.calendarColor() != null) {
            staff.setCalendarColor(request.calendarColor());
        }
    }

    private void ensureEmailAvailable(String email, String exceptStaffId) {
        staffRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (exceptStaffId == null || !existing.getId().equals(exceptStaffId)) {
                throw new ConflictException("A staff member with this email already exists");
            }
        });
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void requireReadAccess(TenantContext ctx) {
        if (isStaffRole(ctx) && ctx.staffId() == null) {
            throw new ForbiddenException("Your account is not linked to a staff profile");
        }
    }

    private void requireManageAccess() {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.STAFF_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage staff");
        }
    }

    private void requireCanView(TenantContext ctx, StaffMember staff) {
        if (ctx.isOwner() || ctx.hasPermission(Permission.STAFF_MANAGE)) {
            return;
        }
        if (isStaffRole(ctx) && staff.getId().equals(ctx.staffId())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to view this staff member");
    }

    private void assertManagerCanModify(TenantContext ctx, StaffMember staff) {
        if (staff.getRole() == StaffRole.OWNER && isManager(ctx)) {
            throw new ForbiddenException("Managers cannot modify the shop owner");
        }
    }

    private void assertCanEditPay(TenantContext ctx) {
        if (isManager(ctx) && !ctx.hasPermission(Permission.STAFF_PAY_MANAGE)) {
            throw new ForbiddenException("Managers cannot change pay settings");
        }
    }

    private static boolean isManager(TenantContext ctx) {
        return "manager".equalsIgnoreCase(ctx.role());
    }

    private static boolean isStaffRole(TenantContext ctx) {
        return "staff".equalsIgnoreCase(ctx.role());
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
