package au.com.getkempt.controllers.authentication;

import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.request.auth.ForgotPasswordRequest;
import au.com.getkempt.request.auth.LoginRequest;
import au.com.getkempt.request.auth.RefreshTokenRequest;
import au.com.getkempt.request.auth.ResetPasswordRequest;
import au.com.getkempt.request.auth.SignupRequest;
import au.com.getkempt.request.auth.VerifyEmailRequest;
import au.com.getkempt.responses.auth.AuthSessionResponse;
import au.com.getkempt.responses.auth.AuthTokensResponse;
import au.com.getkempt.responses.auth.UserResponse;
import au.com.getkempt.services.authentication.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Auth surface — all endpoints are token-minting (no bearer required) except logout/resend. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthSessionResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        return authService.signup(request, httpRequest);
    }

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout(TenantContextHolder.require().sessionId());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public AuthSessionResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                             HttpServletRequest httpRequest) {
        return authService.resetPassword(request, httpRequest);
    }

    @PostMapping("/verify-email")
    public UserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification() {
        authService.resendVerification(TenantContextHolder.require().userId());
    }
}
