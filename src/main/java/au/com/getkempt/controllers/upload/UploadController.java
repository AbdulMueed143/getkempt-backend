package au.com.getkempt.controllers.upload;

import au.com.getkempt.request.upload.UploadPresignRequest;
import au.com.getkempt.responses.upload.UploadConfirmResponse;
import au.com.getkempt.responses.upload.UploadPresignResponse;
import au.com.getkempt.services.upload.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/uploads} — see {@code paths/uploads.yaml}. */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadPresignResponse presignUpload(@Valid @RequestBody UploadPresignRequest request) {
        return uploadService.presign(request);
    }

    @PostMapping("/{uploadId}/confirm")
    public UploadConfirmResponse confirmUpload(@PathVariable String uploadId) {
        return uploadService.confirm(uploadId);
    }
}
