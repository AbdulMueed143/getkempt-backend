package au.com.getkempt.entities.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Enrichment pulled from Google Places for the shop's linked listing —
 * embedded on {@link StoreProfile}, populated at signup from {@code
 * PlacesLookupPort#lookup} (see {@code AuthService#signup}). Null (the whole
 * object, not just fields) when the shop hasn't linked a Google listing, or
 * when the lookup failed at signup time — signup never blocks on this.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreGooglePlace {

    private String placeId;
    private Double rating;
    private Integer userRatingsTotal;
    private String businessStatus;
    private String googleMapsUri;

    @Builder.Default
    private List<StoreGooglePlacePhoto> photos = new ArrayList<>();

    /** When this enrichment was last fetched — surfaced so the frontend can prompt a re-sync if stale. */
    private Instant lastSyncedAt;
}
