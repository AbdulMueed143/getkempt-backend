package au.com.getkempt.repositories.session;

import au.com.getkempt.entities.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Session store: the anchor for refresh-token rotation and the source of truth for
 * access-token revocation (looked up by {@code jti == Session.id} on every request).
 */
@Repository
public interface SessionRepository extends MongoRepository<Session, String> {

    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    Optional<Session> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    List<Session> findByUserIdAndRevokedAtIsNull(String userId);
}
