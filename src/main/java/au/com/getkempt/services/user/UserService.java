package au.com.getkempt.services.user;

import au.com.getkempt.common.error.FieldViolation;
import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.UnauthorizedException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.entities.User;
import au.com.getkempt.mappers.SessionMapper;
import au.com.getkempt.mappers.UserMapper;
import au.com.getkempt.repositories.user.UserRepository;
import au.com.getkempt.request.user.ChangePasswordRequest;
import au.com.getkempt.request.user.UpdateMeRequest;
import au.com.getkempt.responses.auth.SessionResponse;
import au.com.getkempt.responses.auth.UserResponse;
import au.com.getkempt.services.authentication.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/** The currently-authenticated user's own account: profile, password, avatar, sessions. */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String AVATAR_KEY_PREFIX = "avatars/";

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getMe(String userId) {
        return userMapper.toResponse(require(userId));
    }

    public UserResponse updateMe(String userId, UpdateMeRequest request) {
        User user = require(userId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            String newEmail = request.email().trim().toLowerCase(Locale.ROOT);
            if (!newEmail.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ConflictException("An account with this email already exists");
            }
            // Changing email re-arms verification.
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                user.setEmailVerifiedAt(null);
            }
            user.setEmail(newEmail);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = require(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnprocessableEntityException("Current password is incorrect",
                    List.of(FieldViolation.of("currentPassword", "is incorrect")));
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public UserResponse setAvatar(String userId, String uploadId) {
        User user = require(userId);
        user.setAvatarKey(AVATAR_KEY_PREFIX + uploadId);
        return userMapper.toResponse(userRepository.save(user));
    }

    public void deleteAvatar(String userId) {
        User user = require(userId);
        user.setAvatarKey(null);
        userRepository.save(user);
    }

    public List<SessionResponse> listSessions(String userId, String currentSessionId) {
        return sessionService.activeSessions(userId).stream()
                .map(s -> sessionMapper.toResponse(s, currentSessionId))
                .toList();
    }

    public void revokeOtherSessions(String userId, String currentSessionId) {
        sessionService.revokeOtherSessions(userId, currentSessionId);
    }

    private User require(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }
}
