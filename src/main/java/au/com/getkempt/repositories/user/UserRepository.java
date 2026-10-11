package au.com.getkempt.repositories.user;

import au.com.getkempt.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Cross-tenant by design: login resolves a user by email before any shop is known,
 * so this is a plain repository (not a {@code TenantRepository}). Tenant scoping for
 * a user is enforced downstream via its {@code shopId}.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailVerificationTokenHash(String tokenHash);

    Optional<User> findByPasswordResetTokenHash(String tokenHash);
}
