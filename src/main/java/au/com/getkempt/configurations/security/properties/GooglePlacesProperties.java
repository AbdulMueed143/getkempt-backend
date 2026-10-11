package au.com.getkempt.configurations.security.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Config for the (currently stubbed) Google Places integration — see
 * {@code PlacesLookupPort}. Present now, ahead of a real adapter, so the API
 * key/quota surface is already wired into config the same way every other
 * external integration in this codebase is (Stripe, Twilio, SendGrid, ... all
 * follow this "stub first, config always present" convention).
 */
@Configuration
@Data
public class GooglePlacesProperties {

    @Value("${application.configurations.google-places.api-key:}")
    private String apiKey;

    @Value("${application.configurations.google-places.default-photo-max-width-px}")
    private int defaultPhotoMaxWidthPx;
}
