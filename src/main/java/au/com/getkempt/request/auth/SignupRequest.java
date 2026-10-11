package au.com.getkempt.request.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /auth/signup} body. {@code role} is the account intent
 * ({@code barber} | {@code salon_owner}); both become the shop owner.
 *
 * @param placeId The Google {@code placeId} for the shop's listing, selected via the
 *                frontend's Place Autocomplete widget. This is the ONLY location-related
 *                input signup accepts — address, geo-coordinates, rating, and photos are
 *                all resolved server-side from this id (see {@code PlacesLookupPort}),
 *                never free-typed by the owner.
 */
public record SignupRequest(
        @NotBlank @Size(min = 1, max = 50) String firstName,
        @NotBlank @Size(min = 1, max = 50) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 1, max = 100) String businessName,
        @NotBlank @Pattern(regexp = "barber|salon_owner", message = "must be barber or salon_owner") String role,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "must be at least 8 chars and include an uppercase letter and a digit")
        String password,
        @NotBlank String placeId,
        @AssertTrue(message = "you must agree to the terms") boolean agreeToTerms) {
}
