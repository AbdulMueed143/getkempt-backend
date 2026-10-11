package au.com.getkempt.mappers;

import au.com.getkempt.entities.User;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.responses.auth.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maps {@link User} → wire {@code UserResponse}. The avatar URL is resolved from its
 * storage key per-request (never persisted), so rotating the CDN needs no migration.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final FileStorage fileStorage;

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getBusinessName(),
                user.getAvatarKey() == null ? null : fileStorage.publicUrl(user.getAvatarKey()),
                user.getShopId(),
                user.getCreatedAt());
    }
}
