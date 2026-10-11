package au.com.getkempt.request.service;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetServiceStaffRequest(@NotNull List<String> staffIds) {
}
