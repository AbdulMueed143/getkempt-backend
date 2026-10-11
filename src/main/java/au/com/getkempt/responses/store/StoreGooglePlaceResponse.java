package au.com.getkempt.responses.store;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoreGooglePlaceResponse(
        String placeId,
        Double rating,
        Integer userRatingsTotal,
        String businessStatus,
        String googleMapsUri,
        List<StoreGooglePlacePhotoResponse> photos,
        Instant lastSyncedAt) {
}
