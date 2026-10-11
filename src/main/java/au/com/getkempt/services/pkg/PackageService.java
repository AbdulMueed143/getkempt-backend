package au.com.getkempt.services.pkg;

import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.entities.Package;
import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.enums.pkg.PackageStatus;
import au.com.getkempt.mappers.PackageMapper;
import au.com.getkempt.repositories.pkg.PackageRepository;
import au.com.getkempt.repositories.service.ServiceRepository;
import au.com.getkempt.request.pkg.PackageWriteRequest;
import au.com.getkempt.responses.pkg.PackagePricePreviewResponse;
import au.com.getkempt.responses.pkg.PackageResponse;
import au.com.getkempt.services.staff.StaffServiceLinkSync;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/** Package administration with server-derived pricing. */
@Service
@RequiredArgsConstructor
public class PackageService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "name");

    private final PackageRepository packageRepository;
    private final ServiceRepository serviceRepository;
    private final StaffServiceLinkSync linkSync;
    private final PackageMapper packageMapper;
    private final PackagePricingService pricingService;

    public CursorPage<PackageResponse> list(String cursor,
                                            Integer limit,
                                            PackageStatus status,
                                            String staffId) {
        Criteria filter = new Criteria();
        if (status != null) {
            filter.and("status").is(status);
        }
        if (staffId != null && !staffId.isBlank()) {
            filter.and("staffIds").is(staffId);
        }

        SortRequest sort = SortRequest.parse(null, SORTABLE, SortRequest.desc("createdAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return packageRepository.list(filter, sort, page)
                .map(pkg -> packageMapper.toResponse(pkg, loadServices(pkg.getServiceIds())));
    }

    public PackageResponse get(String packageId) {
        Package pkg = packageRepository.requireById(packageId);
        return packageMapper.toResponse(pkg, loadServices(pkg.getServiceIds()));
    }

    public PackageResponse create(PackageWriteRequest request) {
        requireManageAccess();
        List<au.com.getkempt.entities.Service> services = loadServices(request.serviceIds());
        validateDiscount(request);

        Package pkg = new Package();
        applyWrite(pkg, request, true);
        packageRepository.insert(pkg);
        linkSync.validateStaffExist(nullSafe(request.staffIds()));
        pkg.setStaffIds(nullSafe(request.staffIds()));
        packageRepository.save(pkg);
        pricingService.compute(services, pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(), pkg.getDiscountType(), pkg.getDiscountValue());
        return packageMapper.toResponse(pkg, services);
    }

    public PackageResponse update(String packageId, String ifMatch, PackageWriteRequest request) {
        requireManageAccess();
        Package pkg = packageRepository.requireById(packageId);
        IfMatchSupport.check(ifMatch, pkg.getVersion());

        List<au.com.getkempt.entities.Service> services = request.serviceIds() != null
                ? loadServices(request.serviceIds())
                : loadServices(pkg.getServiceIds());
        if (request.discountEnabled() != null || request.discountType() != null || request.discountValue() != null) {
            validateDiscount(request);
        }
        applyWrite(pkg, request, false);
        packageRepository.save(pkg);
        pricingService.compute(services, pkg.getCustomDurationMinutes(),
                pkg.isDiscountEnabled(), pkg.getDiscountType(), pkg.getDiscountValue());
        return packageMapper.toResponse(pkg, services);
    }

    public void delete(String packageId) {
        requireManageAccess();
        packageRepository.softDelete(packageRepository.requireById(packageId));
    }

    public PackageResponse setImage(String packageId, String uploadId) {
        requireManageAccess();
        Package pkg = packageRepository.requireById(packageId);
        pkg.setImage(StoredImageRef.builder().key("packages/" + packageId + "/cover/" + uploadId).build());
        packageRepository.save(pkg);
        return packageMapper.toResponse(pkg, loadServices(pkg.getServiceIds()));
    }

    public void deleteImage(String packageId) {
        requireManageAccess();
        Package pkg = packageRepository.requireById(packageId);
        pkg.setImage(null);
        packageRepository.save(pkg);
    }

    public PackageResponse archive(String packageId) {
        requireManageAccess();
        Package pkg = packageRepository.requireById(packageId);
        pkg.setStatus(PackageStatus.ARCHIVED);
        packageRepository.save(pkg);
        return packageMapper.toResponse(pkg, loadServices(pkg.getServiceIds()));
    }

    public PackageResponse duplicate(String packageId) {
        requireManageAccess();
        Package source = packageRepository.requireById(packageId);
        Package copy = new Package();
        copy.setName(source.getName() + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setImage(source.getImage());
        copy.setServiceIds(nullSafe(source.getServiceIds()));
        copy.setCustomDurationMinutes(source.getCustomDurationMinutes());
        copy.setDiscountEnabled(source.isDiscountEnabled());
        copy.setDiscountType(source.getDiscountType());
        copy.setDiscountValue(source.getDiscountValue());
        copy.setStatus(PackageStatus.INACTIVE);
        copy.setOnlineBookingEnabled(source.isOnlineBookingEnabled());
        copy.setStaffIds(nullSafe(source.getStaffIds()));
        packageRepository.insert(copy);
        return packageMapper.toResponse(copy, loadServices(copy.getServiceIds()));
    }

    public PackagePricePreviewResponse previewPrice(List<String> serviceIds,
                                                    DiscountType discountType,
                                                    Double discountValue,
                                                    Integer customDurationMinutes) {
        List<au.com.getkempt.entities.Service> services = loadServices(serviceIds);
        boolean discountEnabled = discountType != null && discountValue != null;
        PackagePricingService.Pricing pricing = pricingService.compute(
                services, customDurationMinutes, discountEnabled, discountType, discountValue);
        return pricingService.toPreview(pricing);
    }

    private List<au.com.getkempt.entities.Service> loadServices(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.size() < 2) {
            throw new UnprocessableEntityException("A package must contain at least 2 services");
        }
        List<au.com.getkempt.entities.Service> services = serviceRepository.findByIds(serviceIds);
        if (services.size() != serviceIds.size()) {
            for (String id : serviceIds) {
                if (services.stream().noneMatch(s -> s.getId().equals(id))) {
                    throw NotFoundException.of("Service", id);
                }
            }
        }
        return services;
    }

    private void applyWrite(Package pkg, PackageWriteRequest request, boolean creating) {
        if (request.name() != null) {
            pkg.setName(request.name());
        }
        if (request.description() != null) {
            pkg.setDescription(request.description());
        }
        if (request.serviceIds() != null) {
            pkg.setServiceIds(nullSafe(request.serviceIds()));
        }
        if (request.customDurationMinutes() != null) {
            pkg.setCustomDurationMinutes(request.customDurationMinutes());
        }
        if (request.discountEnabled() != null) {
            pkg.setDiscountEnabled(request.discountEnabled());
        } else if (creating) {
            pkg.setDiscountEnabled(false);
        }
        if (request.discountType() != null) {
            pkg.setDiscountType(request.discountType());
        }
        if (request.discountValue() != null) {
            pkg.setDiscountValue(request.discountValue());
        }
        if (request.status() != null) {
            pkg.setStatus(request.status());
        } else if (creating) {
            pkg.setStatus(PackageStatus.ACTIVE);
        }
        if (request.onlineBookingEnabled() != null) {
            pkg.setOnlineBookingEnabled(request.onlineBookingEnabled());
        } else if (creating) {
            pkg.setOnlineBookingEnabled(true);
        }
        if (request.staffIds() != null) {
            linkSync.validateStaffExist(request.staffIds());
            pkg.setStaffIds(nullSafe(request.staffIds()));
        } else if (creating) {
            pkg.setStaffIds(List.of());
        }
    }

    private void validateDiscount(PackageWriteRequest request) {
        if (!Boolean.TRUE.equals(request.discountEnabled())) {
            return;
        }
        if (request.discountType() == null || request.discountValue() == null) {
            throw new UnprocessableEntityException(
                    "discountType and discountValue are required when discount is enabled");
        }
    }

    private void requireManageAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.PACKAGES_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage packages");
        }
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
