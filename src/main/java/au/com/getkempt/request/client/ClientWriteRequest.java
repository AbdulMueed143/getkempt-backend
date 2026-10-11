package au.com.getkempt.request.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Create/update body for a client — mirrors the contract's {@code ClientCreateRequest}
 * ({@code ClientUpdateRequest} is a bare {@code $ref} of it). POST validates the
 * required fields; PATCH reuses the same type without {@code @Valid} and applies
 * only the non-null fields (see {@code ClientService#applyWrite}).
 */
public record ClientWriteRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        @NotBlank String phone,
        String preferredStaffId,
        String notes,
        List<String> tags,
        Boolean marketingOptIn) {
}
