package au.com.getkempt.request.service;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderServicesRequest(@NotNull List<String> serviceIds) {
}
