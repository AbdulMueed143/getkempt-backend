package au.com.getkempt.entities.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Google Place photo reference — deliberately NOT a resolved image URL (see
 * {@code PlacesLookupPort} javadoc: the API key must never reach the client,
 * so only the lightweight reference is persisted; {@code StoreMapper} turns
 * this into a same-origin proxy URL, and {@code StoreController}'s photo
 * endpoint resolves the reference to Google's actual image at request time).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreGooglePlacePhoto {

    private String photoReference;
    private Integer widthPx;
    private Integer heightPx;
}
