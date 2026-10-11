package au.com.getkempt.responses.client;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Wire shape of the contract's {@code Client}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        int totalBookings,
        long lifetimeSpendCents,
        Instant lastBookingDate,
        Instant nextBookingDate,
        String preferredStaffId,
        Instant joinedAt,
        boolean marketingOptIn,
        boolean smsOptIn,
        boolean emailOptIn,
        String notes,
        List<String> tags,
        Instant createdAt,
        Instant deletedAt) {
}
