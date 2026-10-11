package au.com.getkempt.controllers.user;

import au.com.getkempt.common.tenant.TenantContext;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.request.UploadRefRequest;
import au.com.getkempt.request.user.ChangePasswordRequest;
import au.com.getkempt.request.user.UpdateMeRequest;
import au.com.getkempt.responses.auth.SessionResponse;
import au.com.getkempt.responses.auth.UserResponse;
import au.com.getkempt.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The current user's own account ({@code /users/me} ...). Identity comes from the token. */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe() {
        return userService.getMe(currentUserId());
    }

    @PatchMapping
    public UserResponse updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(currentUserId(), request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId(), request);
    }

    @PutMapping("/avatar")
    public UserResponse setAvatar(@Valid @RequestBody UploadRefRequest request) {
        return userService.setAvatar(currentUserId(), request.uploadId());
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() {
        userService.deleteAvatar(currentUserId());
    }

    @GetMapping("/sessions")
    public List<SessionResponse> listSessions() {
        TenantContext ctx = TenantContextHolder.require();
        return userService.listSessions(ctx.userId(), ctx.sessionId());
    }

    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeOtherSessions() {
        TenantContext ctx = TenantContextHolder.require();
        userService.revokeOtherSessions(ctx.userId(), ctx.sessionId());
    }

    private String currentUserId() {
        return TenantContextHolder.require().userId();
    }
}
