package au.com.getkempt.controllers.service;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import au.com.getkempt.request.UploadRefRequest;
import au.com.getkempt.request.service.ReorderServicesRequest;
import au.com.getkempt.request.service.ServiceWriteRequest;
import au.com.getkempt.request.service.SetServiceStaffRequest;
import au.com.getkempt.responses.service.ServiceCategoryResponse;
import au.com.getkempt.responses.service.ServiceResponse;
import au.com.getkempt.services.service.CatalogService;
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
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final CatalogService catalogService;

    @GetMapping
    public CursorPage<ServiceResponse> listServices(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String q) {
        return catalogService.list(cursor, limit, sort, category, status, staffId, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(@Valid @RequestBody ServiceWriteRequest request) {
        return catalogService.create(request);
    }

    @GetMapping("/categories")
    public List<ServiceCategoryResponse> listCategories() {
        return catalogService.categories();
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderServices(@Valid @RequestBody ReorderServicesRequest request) {
        catalogService.reorder(request.serviceIds());
    }

    @GetMapping("/{serviceId}")
    public ServiceResponse getService(@PathVariable String serviceId) {
        return catalogService.get(serviceId);
    }

    @PatchMapping("/{serviceId}")
    public ServiceResponse updateService(
            @PathVariable String serviceId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody ServiceWriteRequest request) {
        return catalogService.update(serviceId, ifMatch, request);
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable String serviceId) {
        catalogService.delete(serviceId);
    }

    @PutMapping("/{serviceId}/image")
    public ServiceResponse setServiceImage(
            @PathVariable String serviceId,
            @Valid @RequestBody UploadRefRequest request) {
        return catalogService.setImage(serviceId, request.uploadId());
    }

    @DeleteMapping("/{serviceId}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServiceImage(@PathVariable String serviceId) {
        catalogService.deleteImage(serviceId);
    }

    @PutMapping("/{serviceId}/staff")
    public ServiceResponse setServiceStaff(
            @PathVariable String serviceId,
            @Valid @RequestBody SetServiceStaffRequest request) {
        return catalogService.setStaff(serviceId, request.staffIds());
    }

    @PostMapping("/{serviceId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse duplicateService(@PathVariable String serviceId) {
        return catalogService.duplicate(serviceId);
    }

    @PostMapping("/{serviceId}/archive")
    public ServiceResponse archiveService(@PathVariable String serviceId) {
        return catalogService.archive(serviceId);
    }
}
