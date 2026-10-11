package au.com.getkempt.request.service;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Create/update body for a service. POST validates required fields; PATCH reuses the
 * type without {@code @Valid} and applies non-null fields only.
 */
public record ServiceWriteRequest(
        @NotBlank String name,
        @NotNull ServiceCategory category,
        String description,
        @NotNull @Min(5) @Max(600) Integer durationMinutes,
        @Min(0) @Max(240) Integer bufferMinutes,
        @NotNull @Valid Money price,
        Boolean depositRequired,
        @Valid Money depositAmount,
        ServiceStatus status,
        Boolean onlineBookingEnabled,
        @NotNull List<String> staffIds) {
}
