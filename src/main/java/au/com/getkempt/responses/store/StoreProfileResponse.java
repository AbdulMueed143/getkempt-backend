package au.com.getkempt.responses.store;

import au.com.getkempt.enums.store.BusinessType;
import au.com.getkempt.responses.ImageRefResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape of the contract's {@code StoreProfile}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoreProfileResponse(
        String id,
        String slug,
        String shopName,
        BusinessType businessType,
        String description,
        ImageRefResponse logo,
        String phone,
        String email,
        String website,
        AddressDto address,
        String timezone,
        String instagram,
        String facebook,
        String tiktok,
        StoreGooglePlaceResponse googlePlace,
        Instant updatedAt) {
}
