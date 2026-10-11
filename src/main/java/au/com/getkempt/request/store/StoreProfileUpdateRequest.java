package au.com.getkempt.request.store;

import au.com.getkempt.enums.store.BusinessType;
import au.com.getkempt.responses.store.AddressDto;
import jakarta.validation.constraints.Email;

/** {@code PATCH /store/profile} body — all fields optional. */
public record StoreProfileUpdateRequest(
        String shopName,
        BusinessType businessType,
        String description,
        String phone,
        @Email String email,
        String website,
        AddressDto address,
        String timezone,
        String instagram,
        String facebook,
        String tiktok) {
}
