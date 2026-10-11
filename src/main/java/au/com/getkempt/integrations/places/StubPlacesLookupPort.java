package au.com.getkempt.integrations.places;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Local Places stub — logs and returns a minimal, structurally valid
 * {@link PlaceDetails} derived deterministically from the given placeId, with
 * no real address/rating/photo data (there's nothing to look up without
 * network access to the real Places API). Exercises every downstream field
 * the real adapter will eventually populate, so {@code AuthService}/{@code
 * StoreMapper} code paths are already correct once a real adapter swaps in.
 */
@Component
@Slf4j
public class StubPlacesLookupPort implements PlacesLookupPort {

    @Override
    public PlaceDetails lookup(String placeId) {
        log.info("[stub-places] lookup placeId={} (no real Places API call — network unavailable)", placeId);
        return new PlaceDetails(
                placeId,
                null,   // name — left to the owner-typed businessName rather than guessed
                null,   // formattedAddress
                null, null, null, null, null,  // line1/suburb/state/postcode/country
                null, null,   // latitude/longitude
                null,   // formattedPhoneNumber
                null,   // websiteUri
                "https://maps.google.com/?place_id=" + placeId,   // googleMapsUri — deterministic, always constructible
                null,   // rating
                null,   // userRatingsTotal
                null,   // businessStatus
                List.of());  // photos
    }

    @Override
    public String resolvePhotoUrl(String photoReference, int maxWidthPx) {
        log.info("[stub-places] resolvePhotoUrl photoReference={} maxWidthPx={}", photoReference, maxWidthPx);
        return "https://places.googleapis.com/v1/" + photoReference + "/media?maxWidthPx=" + maxWidthPx + "&key=stub";
    }
}
