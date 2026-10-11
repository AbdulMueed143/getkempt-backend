package au.com.getkempt.request.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code POST /store/holidays/import} body. */
public record HolidayImportRequest(
        @NotBlank String region,
        @NotNull Integer year) {
}
