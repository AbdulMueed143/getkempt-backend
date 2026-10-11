package au.com.getkempt.responses.availability;

import java.util.List;

/** Wire shape for {@code POST /availability/bulk-update}. */
public record BulkAvailabilityUpdateResponse(List<String> updatedStaffIds) {
}
