package au.com.getkempt.responses.store;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of the contract's {@code StoreAddress}; reused for read and write. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressDto(
        String line1,
        String line2,
        String suburb,
        String state,
        String postcode,
        String country,
        Double latitude,
        Double longitude,
        String placeId) {
}
