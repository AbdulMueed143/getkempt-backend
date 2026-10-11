package au.com.getkempt.responses.booking;

import java.util.List;

public record SlotSearchResponse(List<AvailableSlotResponse> slots) {
}
