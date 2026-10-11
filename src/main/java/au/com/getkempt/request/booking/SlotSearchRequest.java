package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SlotSearchRequest(
        @NotNull LocalDate date,
        String serviceId,
        String packageId,
        String staffId,
        Integer intervalMinutes) {
}
