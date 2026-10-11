package au.com.getkempt.repositories.upload;

import au.com.getkempt.entities.upload.PendingUpload;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingUploadRepository extends MongoRepository<PendingUpload, String> {
}
