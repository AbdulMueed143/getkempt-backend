package au.com.getkempt.mappers;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.entities.store.CancellationFeeRule;
import au.com.getkempt.entities.store.Holiday;
import au.com.getkempt.entities.store.StoreAddress;
import au.com.getkempt.entities.store.StoreGooglePlace;
import au.com.getkempt.entities.store.StoreGooglePlacePhoto;
import au.com.getkempt.entities.store.StoreProfile;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.entities.store.SurchargeRule;
import au.com.getkempt.integrations.storage.FileStorage;
import au.com.getkempt.responses.ImageRefResponse;
import au.com.getkempt.responses.store.AddressDto;
import au.com.getkempt.responses.store.CancellationFeeRuleResponse;
import au.com.getkempt.responses.store.CancellationPolicyResponse;
import au.com.getkempt.responses.store.HolidayResponse;
import au.com.getkempt.responses.store.PrivacyPolicyResponse;
import au.com.getkempt.responses.store.StoreGooglePlacePhotoResponse;
import au.com.getkempt.responses.store.StoreGooglePlaceResponse;
import au.com.getkempt.responses.store.StoreProfileResponse;
import au.com.getkempt.responses.store.StoreSettingsResponse;
import au.com.getkempt.responses.store.SurchargeRuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps the store aggregate (profile + settings + embedded policy lists) → wire DTOs. */
@Component
@RequiredArgsConstructor
public class StoreMapper {

    /** Same-origin path prefix for the Google photo proxy — see {@code StoreController}. */
    private static final String GOOGLE_PHOTO_PROXY_PATH = "/api/v1/store/profile/google-photos/";

    private final FileStorage fileStorage;

    // --- profile ---------------------------------------------------------------

    public StoreProfileResponse toResponse(StoreProfile p) {
        return new StoreProfileResponse(
                p.getId(),
                p.getSlug(),
                p.getShopName(),
                p.getBusinessType(),
                p.getDescription(),
                toImageRef(p.getLogo()),
                p.getPhone(),
                p.getEmail(),
                p.getWebsite(),
                toAddressDto(p.getAddress()),
                p.getTimezone(),
                p.getInstagram(),
                p.getFacebook(),
                p.getTiktok(),
                toResponse(p.getGooglePlace()),
                p.getUpdatedAt());
    }

    /**
     * {@code proxyUrl} is built with a plain string concat (no external call) —
     * only the actual redirect, when the frontend requests it, needs {@code
     * PlacesLookupPort#resolvePhotoUrl}. See {@code StoreGooglePlacePhoto} javadoc.
     */
    public StoreGooglePlaceResponse toResponse(StoreGooglePlace g) {
        if (g == null) {
            return null;
        }
        List<StoreGooglePlacePhotoResponse> photos = nullSafe(g.getPhotos()).stream()
                .map(this::toResponse)
                .toList();
        return new StoreGooglePlaceResponse(
                g.getPlaceId(), g.getRating(), g.getUserRatingsTotal(), g.getBusinessStatus(),
                g.getGoogleMapsUri(), photos, g.getLastSyncedAt());
    }

    private StoreGooglePlacePhotoResponse toResponse(StoreGooglePlacePhoto photo) {
        return new StoreGooglePlacePhotoResponse(
                photo.getPhotoReference(), photo.getWidthPx(), photo.getHeightPx(),
                GOOGLE_PHOTO_PROXY_PATH + photo.getPhotoReference());
    }

    public ImageRefResponse toImageRef(StoredImageRef ref) {
        if (ref == null || ref.getKey() == null) {
            return null;
        }
        return new ImageRefResponse(ref.getKey(), fileStorage.publicUrl(ref.getKey()), ref.getWidth(), ref.getHeight());
    }

    public AddressDto toAddressDto(StoreAddress a) {
        if (a == null) {
            return null;
        }
        return new AddressDto(a.getLine1(), a.getLine2(), a.getSuburb(), a.getState(),
                a.getPostcode(), a.getCountry(), a.getLatitude(), a.getLongitude(), a.getPlaceId());
    }

    public StoreAddress toAddressEntity(AddressDto a) {
        if (a == null) {
            return null;
        }
        return StoreAddress.builder()
                .line1(a.line1()).line2(a.line2()).suburb(a.suburb()).state(a.state())
                .postcode(a.postcode()).country(a.country())
                .latitude(a.latitude()).longitude(a.longitude()).placeId(a.placeId())
                .build();
    }

    // --- settings --------------------------------------------------------------

    public StoreSettingsResponse toResponse(StoreSettings s) {
        return new StoreSettingsResponse(
                s.getBookingWindowValue(),
                s.getBookingWindowUnit(),
                s.getSlotInterval(),
                nullSafe(s.getHolidays()).stream().map(this::toResponse).toList(),
                nullSafe(s.getSurchargeRules()).stream().map(this::toResponse).toList(),
                s.getPaymentMode(),
                s.getDepositType(),
                s.getDepositValue(),
                nullSafe(s.getInStoreMethods()),
                s.isStripeConnected(),
                s.getNoShowFeeType(),
                s.getNoShowFeeValue(),
                s.isAutoCharge(),
                nullSafe(s.getCancellationFeeRules()).stream().map(this::toResponse).toList(),
                s.getCancellationPolicy(),
                s.getPrivacyPolicy(),
                s.getUpdatedAt());
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    // --- holiday ---------------------------------------------------------------

    public HolidayResponse toResponse(Holiday h) {
        Object appliesTo = h.isAppliesToAll()
                ? "all"
                : (h.getAppliesToStaffIds() == null ? List.of() : h.getAppliesToStaffIds());
        return new HolidayResponse(
                h.getId(), h.getName(), h.getDate(), appliesTo,
                h.isHasSurcharge(), h.getSurchargeType(), h.getSurchargeValue());
    }

    // --- surcharge rule --------------------------------------------------------

    public SurchargeRuleResponse toResponse(SurchargeRule r) {
        return new SurchargeRuleResponse(
                r.getId(), r.getName(), r.isActive(), r.getDays(),
                r.getTimeStart(), r.getTimeEnd(), r.getSurchargeType(), r.getSurchargeValue());
    }

    // --- cancellation ----------------------------------------------------------

    public CancellationFeeRuleResponse toResponse(CancellationFeeRule r) {
        return new CancellationFeeRuleResponse(
                r.getId(), r.getLabel(), r.getWithinHours(), r.getFeeType(), r.getFeeValue());
    }

    public CancellationPolicyResponse toCancellationPolicy(StoreSettings s) {
        return new CancellationPolicyResponse(
                s.getCancellationPolicy(),
                s.getNoShowFeeType(),
                s.getNoShowFeeValue(),
                s.isAutoCharge(),
                s.getCancellationFeeRules().stream().map(this::toResponse).toList());
    }

    public PrivacyPolicyResponse toPrivacyPolicy(StoreSettings s) {
        return new PrivacyPolicyResponse(s.getPrivacyPolicy(), s.getPrivacyPolicyUpdatedAt());
    }
}
