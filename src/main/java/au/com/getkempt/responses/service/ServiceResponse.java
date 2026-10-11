package au.com.getkempt.responses.service;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import au.com.getkempt.responses.ImageRefResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code Service}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceResponse(
        String id,
        String name,
        ServiceCategory category,
        String description,
        ImageRefResponse image,
        int durationMinutes,
        int bufferMinutes,
        Money price,
        boolean depositRequired,
        Money depositAmount,
        ServiceStatus status,
        boolean onlineBookingEnabled,
        List<String> staffIds,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt) {
}
