package au.com.getkempt.repositories.notification;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.notification.Notification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link Notification}. */
@Repository
public class NotificationRepository extends TenantRepository<Notification> {

    public NotificationRepository(MongoTemplate template) {
        super(template, Notification.class, IdPrefix.NOTIFICATION);
    }

    public long markAllRead(String recipientUserId) {
        Query query = scopedQuery(Criteria.where("recipientUserId").is(recipientUserId).and("read").is(false));
        return template.updateMulti(query, Update.update("read", true), Notification.class).getModifiedCount();
    }
}
