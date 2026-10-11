package au.com.getkempt.repositories.notification;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.notification.NotificationPreferences;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link NotificationPreferences}; {@code id == userId}. */
@Repository
public class NotificationPreferencesRepository extends TenantRepository<NotificationPreferences> {

    public NotificationPreferencesRepository(MongoTemplate template) {
        super(template, NotificationPreferences.class, IdPrefix.USER);
    }

    public NotificationPreferences getOrCreate(String userId) {
        return findById(userId).orElseGet(() -> {
            NotificationPreferences prefs = new NotificationPreferences();
            prefs.setId(userId);
            return insert(prefs);
        });
    }
}
