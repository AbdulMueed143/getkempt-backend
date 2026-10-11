package au.com.getkempt.services.upload;

import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.upload.PendingUpload;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.repositories.upload.PendingUploadRepository;
import au.com.getkempt.request.upload.UploadPresignRequest;
import au.com.getkempt.responses.upload.UploadConfirmResponse;
import au.com.getkempt.responses.upload.UploadPresignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final PendingUploadRepository pendingUploadRepository;
    private final FileStorage fileStorage;

    public UploadPresignResponse presign(UploadPresignRequest request) {
        FileStorage.PresignedUpload presigned = fileStorage.presign(
                request.purpose().value(), request.filename(), request.contentType(), request.sizeBytes());

        PendingUpload pending = PendingUpload.builder()
                .id(presigned.uploadId())
                .shopId(TenantContextHolder.requireShopId())
                .purpose(request.purpose())
                .filename(request.filename())
                .contentType(request.contentType())
                .sizeBytes(request.sizeBytes())
                .key(presigned.key())
                .confirmed(false)
                .createdAt(Instant.now())
                .build();
        pendingUploadRepository.save(pending);

        return new UploadPresignResponse(presigned.uploadId(), presigned.url(), presigned.fields(), presigned.key(), presigned.expiresAt());
    }

    public UploadConfirmResponse confirm(String uploadId) {
        PendingUpload pending = pendingUploadRepository.findById(uploadId)
                .filter(p -> TenantContextHolder.requireShopId().equals(p.getShopId()))
                .orElseThrow(() -> new NotFoundException(
                        "Upload %s was not found at the expected key (timed out, etc.)".formatted(uploadId)));

        // No real object-storage "head object" / virus-scan / image-processing
        // pipeline exists behind the FileStorage stub, so confirmation here is
        // trust-on-presence rather than a verified existence check; width/height
        // are left null since no real image processing ran.
        pending.setConfirmed(true);
        pendingUploadRepository.save(pending);

        String url = fileStorage.publicUrl(pending.getKey());
        return new UploadConfirmResponse(pending.getKey(), url, null, null);
    }
}
