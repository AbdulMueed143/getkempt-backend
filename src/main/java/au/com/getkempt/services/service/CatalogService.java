package au.com.getkempt.services.service;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.entities.Service;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import au.com.getkempt.mappers.ServiceMapper;
import au.com.getkempt.repositories.service.ServiceRepository;
import au.com.getkempt.request.service.ServiceWriteRequest;
import au.com.getkempt.responses.service.ServiceCategoryResponse;
import au.com.getkempt.responses.service.ServiceResponse;
import au.com.getkempt.services.staff.StaffServiceLinkSync;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Service catalogue administration. */
@Component
@RequiredArgsConstructor
public class CatalogService {

    private static final Set<String> SORTABLE = Set.of("displayOrder", "createdAt", "name");

    private final ServiceRepository serviceRepository;
    private final StaffServiceLinkSync linkSync;
    private final ServiceMapper serviceMapper;

    public CursorPage<ServiceResponse> list(String cursor,
                                            Integer limit,
                                            String sortParam,
                                            ServiceCategory category,
                                            ServiceStatus status,
                                            String staffId,
                                            String q) {
        Criteria filter = new Criteria();
        if (category != null) {
            filter.and("category").is(category);
        }
        if (status != null) {
            filter.and("status").is(status);
        }
        if (staffId != null && !staffId.isBlank()) {
            filter.and("staffIds").is(staffId);
        }
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE);
            filter.and("name").regex(pattern);
        }

        SortRequest sort = SortRequest.parse(sortParam, SORTABLE, SortRequest.asc("displayOrder"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return serviceRepository.list(filter, sort, page).map(serviceMapper::toResponse);
    }

    public ServiceResponse get(String serviceId) {
        return serviceMapper.toResponse(serviceRepository.requireById(serviceId));
    }

    public ServiceResponse create(ServiceWriteRequest request) {
        requireManageAccess();

        Service service = new Service();
        applyWrite(service, request, true);
        service.setDisplayOrder(serviceRepository.maxDisplayOrder() + 1);
        serviceRepository.insert(service);
        linkSync.replaceServiceStaff(service.getId(), nullSafe(request.staffIds()));
        return serviceMapper.toResponse(serviceRepository.requireById(service.getId()));
    }

    public ServiceResponse update(String serviceId, String ifMatch, ServiceWriteRequest request) {
        requireManageAccess();
        Service service = serviceRepository.requireById(serviceId);
        IfMatchSupport.check(ifMatch, service.getVersion());
        applyWrite(service, request, false);
        serviceRepository.save(service);
        if (request.staffIds() != null) {
            linkSync.replaceServiceStaff(serviceId, request.staffIds());
        }
        return serviceMapper.toResponse(serviceRepository.requireById(serviceId));
    }

    public void delete(String serviceId) {
        requireManageAccess();
        Service service = serviceRepository.requireById(serviceId);
        if (serviceRepository.isReferencedByPackage(serviceId)) {
            throw new ConflictException("Service is referenced by an active package and cannot be deleted");
        }
        serviceRepository.softDelete(service);
    }

    public ServiceResponse setImage(String serviceId, String uploadId) {
        requireManageAccess();
        Service service = serviceRepository.requireById(serviceId);
        service.setImage(StoredImageRef.builder().key("services/" + serviceId + "/cover/" + uploadId).build());
        serviceRepository.save(service);
        return serviceMapper.toResponse(service);
    }

    public void deleteImage(String serviceId) {
        requireManageAccess();
        Service service = serviceRepository.requireById(serviceId);
        service.setImage(null);
        serviceRepository.save(service);
    }

    public ServiceResponse setStaff(String serviceId, List<String> staffIds) {
        requireManageAccess();
        serviceRepository.requireById(serviceId);
        linkSync.replaceServiceStaff(serviceId, staffIds);
        return serviceMapper.toResponse(serviceRepository.requireById(serviceId));
    }

    public ServiceResponse duplicate(String serviceId) {
        requireManageAccess();
        Service source = serviceRepository.requireById(serviceId);
        Service copy = new Service();
        copy.setName(source.getName() + " (Copy)");
        copy.setCategory(source.getCategory());
        copy.setDescription(source.getDescription());
        copy.setImage(source.getImage());
        copy.setDurationMinutes(source.getDurationMinutes());
        copy.setBufferMinutes(source.getBufferMinutes());
        copy.setPrice(source.getPrice());
        copy.setDepositRequired(source.isDepositRequired());
        copy.setDepositAmount(source.getDepositAmount());
        copy.setStatus(ServiceStatus.INACTIVE);
        copy.setOnlineBookingEnabled(source.isOnlineBookingEnabled());
        copy.setDisplayOrder(serviceRepository.maxDisplayOrder() + 1);
        serviceRepository.insert(copy);
        linkSync.replaceServiceStaff(copy.getId(), nullSafe(source.getStaffIds()));
        return serviceMapper.toResponse(copy);
    }

    public ServiceResponse archive(String serviceId) {
        requireManageAccess();
        Service service = serviceRepository.requireById(serviceId);
        service.setStatus(ServiceStatus.ARCHIVED);
        serviceRepository.save(service);
        return serviceMapper.toResponse(service);
    }

    public void reorder(List<String> serviceIds) {
        requireManageAccess();
        for (int i = 0; i < serviceIds.size(); i++) {
            Service service = serviceRepository.requireById(serviceIds.get(i));
            service.setDisplayOrder(i);
            serviceRepository.save(service);
        }
    }

    public List<ServiceCategoryResponse> categories() {
        return serviceMapper.categoryCatalog();
    }

    private void applyWrite(Service service, ServiceWriteRequest request, boolean creating) {
        if (request.name() != null) {
            service.setName(request.name());
        }
        if (request.category() != null) {
            service.setCategory(request.category());
        }
        if (request.description() != null) {
            service.setDescription(request.description());
        }
        if (request.durationMinutes() != null) {
            service.setDurationMinutes(request.durationMinutes());
        }
        if (request.bufferMinutes() != null) {
            service.setBufferMinutes(request.bufferMinutes());
        } else if (creating) {
            service.setBufferMinutes(0);
        }
        if (request.price() != null) {
            service.setPrice(request.price());
        } else if (creating) {
            service.setPrice(Money.zero());
        }
        if (request.depositRequired() != null) {
            service.setDepositRequired(request.depositRequired());
        } else if (creating) {
            service.setDepositRequired(false);
        }
        if (request.depositAmount() != null) {
            service.setDepositAmount(request.depositAmount());
        }
        if (request.status() != null) {
            service.setStatus(request.status());
        } else if (creating) {
            service.setStatus(ServiceStatus.ACTIVE);
        }
        if (request.onlineBookingEnabled() != null) {
            service.setOnlineBookingEnabled(request.onlineBookingEnabled());
        } else if (creating) {
            service.setOnlineBookingEnabled(true);
        }
    }

    private void requireManageAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.SERVICES_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage services");
        }
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
