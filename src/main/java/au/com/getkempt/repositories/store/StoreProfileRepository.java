package au.com.getkempt.repositories.store;

import au.com.getkempt.entities.store.StoreProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * One profile per shop ({@code id == shopId}), so tenant isolation is the document
 * identity itself — the service always reads/writes by the caller's shop id.
 */
@Repository
public interface StoreProfileRepository extends MongoRepository<StoreProfile, String> {

    boolean existsBySlug(String slug);

    Optional<StoreProfile> findBySlug(String slug);
}
