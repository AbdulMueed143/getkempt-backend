package au.com.getkempt.controllers.packages;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.enums.pkg.PackageStatus;
import au.com.getkempt.request.UploadRefRequest;
import au.com.getkempt.request.pkg.PackageWriteRequest;
import au.com.getkempt.responses.pkg.PackagePricePreviewResponse;
import au.com.getkempt.responses.pkg.PackageResponse;
import au.com.getkempt.services.pkg.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public CursorPage<PackageResponse> listPackages(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) PackageStatus status,
            @RequestParam(required = false) String staffId) {
        return packageService.list(cursor, limit, status, staffId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackageResponse createPackage(@Valid @RequestBody PackageWriteRequest request) {
        return packageService.create(request);
    }

    @GetMapping("/{packageId}")
    public PackageResponse getPackage(@PathVariable String packageId) {
        return packageService.get(packageId);
    }

    @PatchMapping("/{packageId}")
    public PackageResponse updatePackage(
            @PathVariable String packageId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody PackageWriteRequest request) {
        return packageService.update(packageId, ifMatch, request);
    }

    @DeleteMapping("/{packageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePackage(@PathVariable String packageId) {
        packageService.delete(packageId);
    }

    @PutMapping("/{packageId}/image")
    public PackageResponse setPackageImage(
            @PathVariable String packageId,
            @Valid @RequestBody UploadRefRequest request) {
        return packageService.setImage(packageId, request.uploadId());
    }

    @DeleteMapping("/{packageId}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePackageImage(@PathVariable String packageId) {
        packageService.deleteImage(packageId);
    }

    @PostMapping("/{packageId}/archive")
    public PackageResponse archivePackage(@PathVariable String packageId) {
        return packageService.archive(packageId);
    }

    @PostMapping("/{packageId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageResponse duplicatePackage(@PathVariable String packageId) {
        return packageService.duplicate(packageId);
    }

    @GetMapping("/{packageId}/preview-price")
    public PackagePricePreviewResponse previewPackagePrice(
            @PathVariable String packageId,
            @RequestParam List<String> serviceIds,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) Double discountValue,
            @RequestParam(required = false) Integer customDurationMinutes) {
        return packageService.previewPrice(serviceIds, discountType, discountValue, customDurationMinutes);
    }
}
