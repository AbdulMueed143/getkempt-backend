package au.com.getkempt.responses.store;

/**
 * @param proxyUrl Same-origin URL the frontend should use as the image {@code src} —
 *                 resolves to Google's actual photo server-side, so the API key
 *                 never reaches the client. See {@code StoreController}'s photo endpoint.
 */
public record StoreGooglePlacePhotoResponse(String photoReference, Integer widthPx, Integer heightPx, String proxyUrl) {
}
