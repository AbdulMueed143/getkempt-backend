package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.SurchargeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Create/update body for a holiday. {@code appliesTo} is the contract's
 * oneOf("all" | staffId[]) — accepted as a raw value and normalised in the service.
 */
public record HolidayWriteRequest(
        @NotBlank String name,
        @NotNull LocalDate date,
        Object appliesTo,
        Boolean hasSurcharge,
        SurchargeType surchargeType,
        Double surchargeValue) {
}
