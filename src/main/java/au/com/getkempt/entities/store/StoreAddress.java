package au.com.getkempt.entities.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Embedded postal address for a {@link StoreProfile}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAddress {

    private String line1;
    private String line2;
    private String suburb;
    private String state;
    private String postcode;
    private String country;
    private Double latitude;
    private Double longitude;
    private String placeId;
}
