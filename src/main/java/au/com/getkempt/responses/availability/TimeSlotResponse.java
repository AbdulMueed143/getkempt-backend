package au.com.getkempt.responses.availability;

import au.com.getkempt.enums.availability.SlotType;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of the contract's {@code TimeSlot}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimeSlotResponse(String id, String startTime, String endTime, SlotType type, String label) {
}
