package au.com.getkempt.integrations.places;

import java.util.List;

/**
 * Port for Google Places (Places API "Place Details" + Place Photos, in
 * production). Domain code depends only on this interface, never on a Google
 * Maps SDK, so store-provisioning logic stays testable and vendor-agnostic —
 * same reasoning as {@code PaymentGateway}/{@code EmailSender}. A local stub
 * implements it until the real adapter lands.
 *
 * <p>The frontend only ever collects a {@code placeId} (from Google's Place
 * Autocomplete widget) — never raw address text, lat/lng, photos, or rating.
 * All of that is resolved server-side via {@link #lookup} so a shop's address
 * data is authoritative from Google rather than free-typed.
 */
public interface PlacesLookupPort {

    record PlacePhoto(String photoReference, Integer widthPx, Integer heightPx) {
    }

    record PlaceDetails(
            String placeId,
            String name,
            String formattedAddress,
            String line1,
            String suburb,
            String state,
            String postcode,
            String country,
            Double latitude,
            Double longitude,
            String formattedPhoneNumber,
            String websiteUri,
            String googleMapsUri,
            Double rating,
            Integer userRatingsTotal,
            String businessStatus,
            List<PlacePhoto> photos) {
    }

    /** Resolves a Google {@code placeId} to full place details. Throws if the id is invalid/not found. */
    PlaceDetails lookup(String placeId);

    /**
     * Resolves a {@code photoReference} (from {@link PlaceDetails#photos()}) to an
     * actual, directly-fetchable image URL, with the API key applied server-side.
     * Callers should NEVER hand this URL to the frontend directly — see
     * {@code StoreController}'s photo-proxy endpoint, which is what the frontend
     * is given instead (so the API key never reaches the client).
     */
    String resolvePhotoUrl(String photoReference, int maxWidthPx);
}
