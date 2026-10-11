package au.com.getkempt.repositories.dashboard;

import au.com.getkempt.entities.dashboard.DismissedAlert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DismissedAlertRepository extends MongoRepository<DismissedAlert, String> {

    List<DismissedAlert> findByShopId(String shopId);

    boolean existsByShopIdAndAlertKey(String shopId, String alertKey);
}
