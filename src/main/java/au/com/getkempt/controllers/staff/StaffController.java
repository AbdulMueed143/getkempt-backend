package au.com.getkempt.controllers.staff;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.enums.staff.StaffStatus;
import au.com.getkempt.request.UploadRefRequest;
import au.com.getkempt.request.staff.CreateInvitationRequest;
import au.com.getkempt.request.staff.SetStaffServicesRequest;
import au.com.getkempt.request.staff.StaffWriteRequest;
import au.com.getkempt.responses.staff.StaffInvitationResponse;
import au.com.getkempt.responses.staff.StaffResponse;
import au.com.getkempt.services.staff.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public CursorPage<StaffResponse> listStaff(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) StaffStatus status,
            @RequestParam(required = false) StaffRole role,
            @RequestParam(required = false) String q) {
        return staffService.list(cursor, limit, sort, status, role, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@Valid @RequestBody StaffWriteRequest request) {
        return staffService.create(request);
    }

    @GetMapping("/{staffId}")
    public StaffResponse getStaff(@PathVariable String staffId) {
        return staffService.get(staffId);
    }

    @PatchMapping("/{staffId}")
    public StaffResponse updateStaff(
            @PathVariable String staffId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody StaffWriteRequest request) {
        return staffService.update(staffId, ifMatch, request);
    }

    @DeleteMapping("/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStaff(@PathVariable String staffId) {
        staffService.delete(staffId);
    }

    @PutMapping("/{staffId}/services")
    public StaffResponse setStaffServices(
            @PathVariable String staffId,
            @Valid @RequestBody SetStaffServicesRequest request) {
        return staffService.setServices(staffId, request.serviceIds());
    }

    @PutMapping("/{staffId}/avatar")
    public StaffResponse setStaffAvatar(
            @PathVariable String staffId,
            @Valid @RequestBody UploadRefRequest request) {
        return staffService.setAvatar(staffId, request.uploadId());
    }

    @DeleteMapping("/{staffId}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStaffAvatar(@PathVariable String staffId) {
        staffService.deleteAvatar(staffId);
    }

    @PostMapping("/{staffId}/deactivate")
    public StaffResponse deactivateStaff(@PathVariable String staffId) {
        return staffService.deactivate(staffId);
    }

    @PostMapping("/{staffId}/reactivate")
    public StaffResponse reactivateStaff(@PathVariable String staffId) {
        return staffService.reactivate(staffId);
    }

    @GetMapping("/invitations")
    public List<StaffInvitationResponse> listInvitations() {
        return staffService.listInvitations();
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffInvitationResponse createInvitation(@Valid @RequestBody CreateInvitationRequest request) {
        return staffService.createInvitation(request);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@PathVariable String invitationId) {
        staffService.revokeInvitation(invitationId);
    }

    @PostMapping("/invitations/{invitationId}/resend")
    public StaffInvitationResponse resendInvitation(@PathVariable String invitationId) {
        return staffService.resendInvitation(invitationId);
    }
}
