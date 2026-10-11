package au.com.getkempt.services.store;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.PreconditionFailedException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.configurations.security.properties.GooglePlacesProperties;
import au.com.getkempt.entities.store.CancellationFeeRule;
import au.com.getkempt.entities.store.Holiday;
import au.com.getkempt.entities.store.StoreProfile;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.entities.store.SurchargeRule;
import au.com.getkempt.integrations.places.PlacesLookupPort;
import au.com.getkempt.mappers.StoreMapper;
import au.com.getkempt.repositories.store.StoreProfileRepository;
import au.com.getkempt.repositories.store.StoreSettingsRepository;
import au.com.getkempt.request.store.CancellationFeeRuleRequest;
import au.com.getkempt.request.store.HolidayImportRequest;
import au.com.getkempt.request.store.HolidayWriteRequest;
import au.com.getkempt.request.store.SetCancellationPolicyRequest;
import au.com.getkempt.request.store.SetPrivacyPolicyRequest;
import au.com.getkempt.request.store.StoreProfileUpdateRequest;
import au.com.getkempt.request.store.StoreSettingsUpdateRequest;
import au.com.getkempt.request.store.SurchargeRuleRequest;
import au.com.getkempt.responses.store.CancellationFeeRuleResponse;
import au.com.getkempt.responses.store.CancellationPolicyResponse;
import au.com.getkempt.responses.store.HolidayImportResponse;
import au.com.getkempt.responses.store.HolidayResponse;
import au.com.getkempt.responses.store.PrivacyPolicyResponse;
import au.com.getkempt.responses.store.StoreProfileResponse;
import au.com.getkempt.responses.store.StoreSettingsResponse;
import au.com.getkempt.responses.store.SurchargeRuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Tenant store administration. Every operation is scoped to the caller's shop via
 * {@link TenantContextHolder#requireShopId()} (the profile/settings {@code id == shopId}).
 * Profile + settings carry {@code @Version}; the {@code If-Match} header is checked
 * against it to surface concurrent edits as 412.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreProfileRepository storeProfileRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final StoreMapper storeMapper;
    private final PlacesLookupPort placesLookupPort;
    private final GooglePlacesProperties googlePlacesProperties;

    private static final String AVATAR_LOGO_PREFIX = "logos/";

    // --- profile ---------------------------------------------------------------

    public StoreProfileResponse getProfile() {
        return storeMapper.toResponse(requireProfile());
    }

    public StoreProfileResponse updateProfile(String ifMatch, StoreProfileUpdateRequest request) {
        StoreProfile profile = requireProfile();
        checkVersion(ifMatch, profile.getVersion());

        if (request.shopName() != null) {
            profile.setShopName(request.shopName());
        }
        if (request.businessType() != null) {
            profile.setBusinessType(request.businessType());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        if (request.phone() != null) {
            profile.setPhone(request.phone());
        }
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.website() != null) {
            profile.setWebsite(request.website());
        }
        if (request.address() != null) {
            profile.setAddress(storeMapper.toAddressEntity(request.address()));
        }
        if (request.timezone() != null) {
            profile.setTimezone(request.timezone());
        }
        if (request.instagram() != null) {
            profile.setInstagram(request.instagram());
        }
        if (request.facebook() != null) {
            profile.setFacebook(request.facebook());
        }
        if (request.tiktok() != null) {
            profile.setTiktok(request.tiktok());
        }
        return storeMapper.toResponse(storeProfileRepository.save(profile));
    }

    public StoreProfileResponse setLogo(String uploadId) {
        StoreProfile profile = requireProfile();
        profile.setLogo(StoredImageRef.builder().key(AVATAR_LOGO_PREFIX + uploadId).build());
        return storeMapper.toResponse(storeProfileRepository.save(profile));
    }

    public void deleteLogo() {
        StoreProfile profile = requireProfile();
        profile.setLogo(null);
        storeProfileRepository.save(profile);
    }

    /**
     * Resolves a Google Place photo reference to the actual image URL, for
     * {@code StoreController} to 302-redirect to — this is the ONLY place the
     * Google Places API key is ever used, and it never leaves the backend (see
     * {@code PlacesLookupPort} javadoc). Requires the reference to actually
     * belong to this shop's linked place, so one shop can't use another's
     * reference to ride along on this endpoint.
     */
    public String resolveGooglePhotoUrl(String photoReference, Integer maxWidthPx) {
        StoreProfile profile = requireProfile();
        boolean owned = profile.getGooglePlace() != null && profile.getGooglePlace().getPhotos() != null
                && profile.getGooglePlace().getPhotos().stream().anyMatch(p -> photoReference.equals(p.getPhotoReference()));
        if (!owned) {
            throw NotFoundException.of("StoreGooglePlacePhoto", photoReference);
        }
        int width = maxWidthPx != null ? maxWidthPx : googlePlacesProperties.getDefaultPhotoMaxWidthPx();
        return placesLookupPort.resolvePhotoUrl(photoReference, width);
    }

    // --- settings --------------------------------------------------------------

    public StoreSettingsResponse getSettings() {
        return storeMapper.toResponse(requireSettings());
    }

    public StoreSettingsResponse updateSettings(String ifMatch, StoreSettingsUpdateRequest request) {
        StoreSettings settings = requireSettings();
        checkVersion(ifMatch, settings.getVersion());

        if (request.bookingWindowValue() != null) {
            settings.setBookingWindowValue(request.bookingWindowValue());
        }
        if (request.bookingWindowUnit() != null) {
            settings.setBookingWindowUnit(request.bookingWindowUnit());
        }
        if (request.slotInterval() != null) {
            settings.setSlotInterval(request.slotInterval());
        }
        if (request.paymentMode() != null) {
            settings.setPaymentMode(request.paymentMode());
        }
        if (request.depositType() != null) {
            settings.setDepositType(request.depositType());
        }
        if (request.depositValue() != null) {
            settings.setDepositValue(request.depositValue());
        }
        if (request.inStoreMethods() != null) {
            settings.setInStoreMethods(request.inStoreMethods());
        }
        if (request.noShowFeeType() != null) {
            settings.setNoShowFeeType(request.noShowFeeType());
        }
        if (request.noShowFeeValue() != null) {
            settings.setNoShowFeeValue(request.noShowFeeValue());
        }
        if (request.autoCharge() != null) {
            settings.setAutoCharge(request.autoCharge());
        }
        if (request.cancellationPolicy() != null) {
            settings.setCancellationPolicy(request.cancellationPolicy());
        }
        if (request.privacyPolicy() != null) {
            settings.setPrivacyPolicy(request.privacyPolicy());
            settings.setPrivacyPolicyUpdatedAt(Instant.now());
        }
        return storeMapper.toResponse(storeSettingsRepository.save(settings));
    }

    // --- holidays --------------------------------------------------------------

    public List<HolidayResponse> listHolidays() {
        return requireSettings().getHolidays().stream().map(storeMapper::toResponse).toList();
    }

    public HolidayResponse createHoliday(HolidayWriteRequest request) {
        StoreSettings settings = requireSettings();
        Holiday holiday = applyHoliday(new Holiday(), request);
        holiday.setId(IdGenerator.generate(IdPrefix.HOLIDAY));
        settings.getHolidays().add(holiday);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(holiday);
    }

    public HolidayResponse updateHoliday(String holidayId, HolidayWriteRequest request) {
        StoreSettings settings = requireSettings();
        Holiday holiday = settings.getHolidays().stream()
                .filter(h -> h.getId().equals(holidayId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Holiday", holidayId));
        applyHoliday(holiday, request);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(holiday);
    }

    public void deleteHoliday(String holidayId) {
        StoreSettings settings = requireSettings();
        boolean removed = settings.getHolidays().removeIf(h -> h.getId().equals(holidayId));
        if (!removed) {
            throw NotFoundException.of("Holiday", holidayId);
        }
        storeSettingsRepository.save(settings);
    }

    public HolidayImportResponse importHolidays(HolidayImportRequest request) {
        StoreSettings settings = requireSettings();
        List<HolidayResponse> imported = new ArrayList<>();
        int skipped = 0;

        for (Holiday candidate : publicHolidays(request.year())) {
            boolean exists = settings.getHolidays().stream()
                    .anyMatch(h -> candidate.getDate().equals(h.getDate()));
            if (exists) {
                skipped++;
                continue;
            }
            candidate.setId(IdGenerator.generate(IdPrefix.HOLIDAY));
            settings.getHolidays().add(candidate);
            imported.add(storeMapper.toResponse(candidate));
        }
        storeSettingsRepository.save(settings);
        return new HolidayImportResponse(imported.size(), skipped, imported);
    }

    // --- surcharge rules -------------------------------------------------------

    public List<SurchargeRuleResponse> listSurchargeRules() {
        return requireSettings().getSurchargeRules().stream().map(storeMapper::toResponse).toList();
    }

    public SurchargeRuleResponse createSurchargeRule(SurchargeRuleRequest request) {
        StoreSettings settings = requireSettings();
        SurchargeRule rule = applySurcharge(new SurchargeRule(), request);
        rule.setId(IdGenerator.generate(IdPrefix.SURCHARGE_RULE));
        settings.getSurchargeRules().add(rule);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(rule);
    }

    public SurchargeRuleResponse updateSurchargeRule(String ruleId, SurchargeRuleRequest request) {
        StoreSettings settings = requireSettings();
        SurchargeRule rule = settings.getSurchargeRules().stream()
                .filter(r -> r.getId().equals(ruleId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("SurchargeRule", ruleId));
        applySurcharge(rule, request);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(rule);
    }

    public void deleteSurchargeRule(String ruleId) {
        StoreSettings settings = requireSettings();
        boolean removed = settings.getSurchargeRules().removeIf(r -> r.getId().equals(ruleId));
        if (!removed) {
            throw NotFoundException.of("SurchargeRule", ruleId);
        }
        storeSettingsRepository.save(settings);
    }

    // --- cancellation policy + fee rules --------------------------------------

    public CancellationPolicyResponse getCancellationPolicy() {
        return storeMapper.toCancellationPolicy(requireSettings());
    }

    public CancellationPolicyResponse setCancellationPolicy(SetCancellationPolicyRequest request) {
        StoreSettings settings = requireSettings();
        settings.setCancellationPolicy(request.policyText());
        if (request.noShowFeeType() != null) {
            settings.setNoShowFeeType(request.noShowFeeType());
        }
        if (request.noShowFeeValue() != null) {
            settings.setNoShowFeeValue(request.noShowFeeValue());
        }
        if (request.autoCharge() != null) {
            settings.setAutoCharge(request.autoCharge());
        }
        List<CancellationFeeRule> rules = new ArrayList<>();
        if (request.rules() != null) {
            for (CancellationFeeRuleRequest r : request.rules()) {
                CancellationFeeRule rule = applyCancellation(new CancellationFeeRule(), r);
                rule.setId(IdGenerator.generate(IdPrefix.CANCELLATION_RULE));
                rules.add(rule);
            }
        }
        settings.setCancellationFeeRules(rules);
        storeSettingsRepository.save(settings);
        return storeMapper.toCancellationPolicy(settings);
    }

    public List<CancellationFeeRuleResponse> listCancellationFeeRules() {
        return requireSettings().getCancellationFeeRules().stream().map(storeMapper::toResponse).toList();
    }

    public CancellationFeeRuleResponse createCancellationFeeRule(CancellationFeeRuleRequest request) {
        StoreSettings settings = requireSettings();
        CancellationFeeRule rule = applyCancellation(new CancellationFeeRule(), request);
        rule.setId(IdGenerator.generate(IdPrefix.CANCELLATION_RULE));
        settings.getCancellationFeeRules().add(rule);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(rule);
    }

    public CancellationFeeRuleResponse updateCancellationFeeRule(String ruleId, CancellationFeeRuleRequest request) {
        StoreSettings settings = requireSettings();
        CancellationFeeRule rule = settings.getCancellationFeeRules().stream()
                .filter(r -> r.getId().equals(ruleId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("CancellationFeeRule", ruleId));
        applyCancellation(rule, request);
        storeSettingsRepository.save(settings);
        return storeMapper.toResponse(rule);
    }

    public void deleteCancellationFeeRule(String ruleId) {
        StoreSettings settings = requireSettings();
        boolean removed = settings.getCancellationFeeRules().removeIf(r -> r.getId().equals(ruleId));
        if (!removed) {
            throw NotFoundException.of("CancellationFeeRule", ruleId);
        }
        storeSettingsRepository.save(settings);
    }

    // --- privacy policy --------------------------------------------------------

    public PrivacyPolicyResponse getPrivacyPolicy() {
        return storeMapper.toPrivacyPolicy(requireSettings());
    }

    public PrivacyPolicyResponse setPrivacyPolicy(SetPrivacyPolicyRequest request) {
        StoreSettings settings = requireSettings();
        settings.setPrivacyPolicy(request.text());
        settings.setPrivacyPolicyUpdatedAt(Instant.now());
        storeSettingsRepository.save(settings);
        return storeMapper.toPrivacyPolicy(settings);
    }

    // --- helpers ---------------------------------------------------------------

    private StoreProfile requireProfile() {
        String shopId = TenantContextHolder.requireShopId();
        return storeProfileRepository.findById(shopId)
                .orElseThrow(() -> NotFoundException.of("StoreProfile", shopId));
    }

    private StoreSettings requireSettings() {
        String shopId = TenantContextHolder.requireShopId();
        return storeSettingsRepository.findById(shopId)
                .orElseThrow(() -> NotFoundException.of("StoreSettings", shopId));
    }

    private void checkVersion(String ifMatch, Long currentVersion) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return;
        }
        long expected;
        try {
            expected = Long.parseLong(ifMatch.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Malformed If-Match header");
        }
        if (currentVersion != null && expected != currentVersion) {
            throw new PreconditionFailedException(
                    "Resource has changed since version %d; reload and retry".formatted(expected));
        }
    }

    @SuppressWarnings("unchecked")
    private Holiday applyHoliday(Holiday holiday, HolidayWriteRequest request) {
        holiday.setName(request.name());
        holiday.setDate(request.date());
        holiday.setHasSurcharge(Boolean.TRUE.equals(request.hasSurcharge()));
        holiday.setSurchargeType(request.surchargeType());
        holiday.setSurchargeValue(request.surchargeValue());

        Object appliesTo = request.appliesTo();
        if (appliesTo == null || "all".equals(appliesTo)) {
            holiday.setAppliesToAll(true);
            holiday.setAppliesToStaffIds(null);
        } else if (appliesTo instanceof List<?> list) {
            holiday.setAppliesToAll(false);
            holiday.setAppliesToStaffIds(list.stream().map(String::valueOf).toList());
        } else {
            throw new BadRequestException("appliesTo must be \"all\" or an array of staff ids");
        }
        return holiday;
    }

    private SurchargeRule applySurcharge(SurchargeRule rule, SurchargeRuleRequest request) {
        rule.setName(request.name());
        rule.setActive(!Boolean.FALSE.equals(request.isActive()));
        rule.setDays(request.days());
        rule.setTimeStart(request.timeStart());
        rule.setTimeEnd(request.timeEnd());
        rule.setSurchargeType(request.surchargeType());
        rule.setSurchargeValue(request.surchargeValue());
        return rule;
    }

    private CancellationFeeRule applyCancellation(CancellationFeeRule rule, CancellationFeeRuleRequest request) {
        rule.setLabel(request.label());
        rule.setWithinHours(request.withinHours());
        rule.setFeeType(request.feeType());
        rule.setFeeValue(request.feeValue());
        return rule;
    }

    /**
     * Minimal national AU public-holiday seed. TODO(phase-later): replace with a real
     * regional provider keyed on {@code request.region()}.
     */
    private List<Holiday> publicHolidays(int year) {
        return List.of(
                fixedHoliday("New Year's Day", LocalDate.of(year, Month.JANUARY, 1)),
                fixedHoliday("Australia Day", LocalDate.of(year, Month.JANUARY, 26)),
                fixedHoliday("Christmas Day", LocalDate.of(year, Month.DECEMBER, 25)),
                fixedHoliday("Boxing Day", LocalDate.of(year, Month.DECEMBER, 26)));
    }

    private Holiday fixedHoliday(String name, LocalDate date) {
        return Holiday.builder().name(name).date(date).appliesToAll(true).hasSurcharge(false).build();
    }
}
