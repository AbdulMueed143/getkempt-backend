package au.com.getkempt.repositories.store;

import au.com.getkempt.entities.store.StoreSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/** One settings document per shop ({@code id == shopId}). */
@Repository
public interface StoreSettingsRepository extends MongoRepository<StoreSettings, String> {
}
