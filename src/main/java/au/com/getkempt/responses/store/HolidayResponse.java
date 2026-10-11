package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.SurchargeType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * Wire shape of the contract's {@code Holiday}. {@code appliesTo} is the oneOf
 * ("all" | staffId[]) re-assembled from the entity's normalised fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HolidayResponse(
        String id,
        String name,
        LocalDate date,
        Object appliesTo,
        boolean hasSurcharge,
        SurchargeType surchargeType,
        Double surchargeValue) {
}
