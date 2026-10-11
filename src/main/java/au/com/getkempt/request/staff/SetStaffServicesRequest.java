package au.com.getkempt.request.staff;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetStaffServicesRequest(@NotNull List<String> serviceIds) {
}
