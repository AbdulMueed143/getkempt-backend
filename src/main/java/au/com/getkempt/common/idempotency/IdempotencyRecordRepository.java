package au.com.getkempt.common.idempotency;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByShopIdAndIdempotencyKey(String shopId, String idempotencyKey);
}
