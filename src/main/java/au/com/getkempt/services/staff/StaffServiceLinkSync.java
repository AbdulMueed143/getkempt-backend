package au.com.getkempt.services.staff;

import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.entities.Service;
import au.com.getkempt.entities.StaffMember;
import au.com.getkempt.repositories.service.ServiceRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the bidirectional staff ↔ service link in sync ({@code StaffMember.services}
 * and {@code Service.staffIds}). Callers update one side; this component updates the other.
 */
@Component
@RequiredArgsConstructor
public class StaffServiceLinkSync {

    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;

    public void replaceStaffServices(String staffId, List<String> serviceIds) {
        StaffMember staff = staffRepository.requireById(staffId);
        List<String> normalized = normalizeIds(serviceIds);
        validateServicesExist(normalized);

        Set<String> before = new HashSet<>(nullSafe(staff.getServices()));
        Set<String> after = new HashSet<>(normalized);

        staff.setServices(new ArrayList<>(after));
        staffRepository.save(staff);

        for (String removed : before) {
            if (!after.contains(removed)) {
                removeStaffFromService(removed, staffId);
            }
        }
        for (String added : after) {
            if (!before.contains(added)) {
                addStaffToService(added, staffId);
            }
        }
    }

    public void replaceServiceStaff(String serviceId, List<String> staffIds) {
        Service service = serviceRepository.requireById(serviceId);
        List<String> normalized = normalizeIds(staffIds);
        validateStaffExist(normalized);

        Set<String> before = new HashSet<>(nullSafe(service.getStaffIds()));
        Set<String> after = new HashSet<>(normalized);

        service.setStaffIds(new ArrayList<>(after));
        serviceRepository.save(service);

        for (String removed : before) {
            if (!after.contains(removed)) {
                removeServiceFromStaff(removed, serviceId);
            }
        }
        for (String added : after) {
            if (!before.contains(added)) {
                addServiceToStaff(added, serviceId);
            }
        }
    }

    public void validateServicesExist(List<String> serviceIds) {
        for (String id : serviceIds) {
            if (!serviceRepository.existsById(id)) {
                throw NotFoundException.of("Service", id);
            }
        }
    }

    public void validateStaffExist(List<String> staffIds) {
        for (String id : staffIds) {
            if (!staffRepository.existsById(id)) {
                throw NotFoundException.of("StaffMember", id);
            }
        }
    }

    private void addStaffToService(String serviceId, String staffId) {
        Service service = serviceRepository.requireById(serviceId);
        List<String> ids = new ArrayList<>(nullSafe(service.getStaffIds()));
        if (!ids.contains(staffId)) {
            ids.add(staffId);
            service.setStaffIds(ids);
            serviceRepository.save(service);
        }
    }

    private void removeStaffFromService(String serviceId, String staffId) {
        Service service = serviceRepository.requireById(serviceId);
        List<String> ids = new ArrayList<>(nullSafe(service.getStaffIds()));
        if (ids.remove(staffId)) {
            service.setStaffIds(ids);
            serviceRepository.save(service);
        }
    }

    private void addServiceToStaff(String staffId, String serviceId) {
        StaffMember staff = staffRepository.requireById(staffId);
        List<String> ids = new ArrayList<>(nullSafe(staff.getServices()));
        if (!ids.contains(serviceId)) {
            ids.add(serviceId);
            staff.setServices(ids);
            staffRepository.save(staff);
        }
    }

    private void removeServiceFromStaff(String staffId, String serviceId) {
        StaffMember staff = staffRepository.requireById(staffId);
        List<String> ids = new ArrayList<>(nullSafe(staff.getServices()));
        if (ids.remove(serviceId)) {
            staff.setServices(ids);
            staffRepository.save(staff);
        }
    }

    private static List<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            throw new UnprocessableEntityException("Ids list must not be null");
        }
        return ids.stream().distinct().toList();
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
