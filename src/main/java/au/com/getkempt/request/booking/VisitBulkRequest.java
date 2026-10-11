package au.com.getkempt.request.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record VisitBulkRequest(
        @NotEmpty List<String> appointmentIds,
        @NotBlank String action,
        String note) {
}
