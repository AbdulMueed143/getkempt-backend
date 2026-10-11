package au.com.getkempt.controllers.store;

import au.com.getkempt.request.UploadRefRequest;
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
import au.com.getkempt.services.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Tenant store admin: profile, settings, holidays, surcharges, cancellation + privacy policy. */
@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // --- profile ---------------------------------------------------------------

    @GetMapping("/profile")
    public StoreProfileResponse getProfile() {
        return storeService.getProfile();
    }

    @PatchMapping("/profile")
    public StoreProfileResponse updateProfile(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody StoreProfileUpdateRequest request) {
        return storeService.updateProfile(ifMatch, request);
    }

    @PutMapping("/profile/logo")
    public StoreProfileResponse setLogo(@Valid @RequestBody UploadRefRequest request) {
        return storeService.setLogo(request.uploadId());
    }

    @DeleteMapping("/profile/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLogo() {
        storeService.deleteLogo();
    }

    /**
     * Proxies a Google Place photo. The frontend is only ever given this
     * same-origin URL (see {@code StoreMapper}) — never a Google URL bearing
     * the API key — and gets redirected on to the real image at request time.
     */
    @GetMapping("/profile/google-photos/{photoReference}")
    public ResponseEntity<Void> getGooglePhoto(
            @PathVariable String photoReference,
            @RequestParam(required = false) Integer maxWidthPx) {
        String url = storeService.resolveGooglePhotoUrl(photoReference, maxWidthPx);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    // --- settings --------------------------------------------------------------

    @GetMapping("/settings")
    public StoreSettingsResponse getSettings() {
        return storeService.getSettings();
    }

    @PatchMapping("/settings")
    public StoreSettingsResponse updateSettings(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody StoreSettingsUpdateRequest request) {
        return storeService.updateSettings(ifMatch, request);
    }

    // --- holidays --------------------------------------------------------------

    @GetMapping("/holidays")
    public List<HolidayResponse> listHolidays() {
        return storeService.listHolidays();
    }

    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse createHoliday(@Valid @RequestBody HolidayWriteRequest request) {
        return storeService.createHoliday(request);
    }

    @PatchMapping("/holidays/{holidayId}")
    public HolidayResponse updateHoliday(@PathVariable String holidayId,
                                         @Valid @RequestBody HolidayWriteRequest request) {
        return storeService.updateHoliday(holidayId, request);
    }

    @DeleteMapping("/holidays/{holidayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable String holidayId) {
        storeService.deleteHoliday(holidayId);
    }

    @PostMapping("/holidays/import")
    public HolidayImportResponse importHolidays(@Valid @RequestBody HolidayImportRequest request) {
        return storeService.importHolidays(request);
    }

    // --- surcharge rules -------------------------------------------------------

    @GetMapping("/surcharge-rules")
    public List<SurchargeRuleResponse> listSurchargeRules() {
        return storeService.listSurchargeRules();
    }

    @PostMapping("/surcharge-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public SurchargeRuleResponse createSurchargeRule(@Valid @RequestBody SurchargeRuleRequest request) {
        return storeService.createSurchargeRule(request);
    }

    @PatchMapping("/surcharge-rules/{ruleId}")
    public SurchargeRuleResponse updateSurchargeRule(@PathVariable String ruleId,
                                                     @Valid @RequestBody SurchargeRuleRequest request) {
        return storeService.updateSurchargeRule(ruleId, request);
    }

    @DeleteMapping("/surcharge-rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSurchargeRule(@PathVariable String ruleId) {
        storeService.deleteSurchargeRule(ruleId);
    }

    // --- cancellation policy + rules ------------------------------------------

    @GetMapping("/cancellation-policy")
    public CancellationPolicyResponse getCancellationPolicy() {
        return storeService.getCancellationPolicy();
    }

    @PutMapping("/cancellation-policy")
    public CancellationPolicyResponse setCancellationPolicy(@Valid @RequestBody SetCancellationPolicyRequest request) {
        return storeService.setCancellationPolicy(request);
    }

    @GetMapping("/cancellation-policy/rules")
    public List<CancellationFeeRuleResponse> listCancellationFeeRules() {
        return storeService.listCancellationFeeRules();
    }

    @PostMapping("/cancellation-policy/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public CancellationFeeRuleResponse createCancellationFeeRule(
            @Valid @RequestBody CancellationFeeRuleRequest request) {
        return storeService.createCancellationFeeRule(request);
    }

    @PatchMapping("/cancellation-policy/rules/{ruleId}")
    public CancellationFeeRuleResponse updateCancellationFeeRule(
            @PathVariable String ruleId, @Valid @RequestBody CancellationFeeRuleRequest request) {
        return storeService.updateCancellationFeeRule(ruleId, request);
    }

    @DeleteMapping("/cancellation-policy/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCancellationFeeRule(@PathVariable String ruleId) {
        storeService.deleteCancellationFeeRule(ruleId);
    }

    // --- privacy policy --------------------------------------------------------

    @GetMapping("/privacy-policy")
    public PrivacyPolicyResponse getPrivacyPolicy() {
        return storeService.getPrivacyPolicy();
    }

    @PutMapping("/privacy-policy")
    public PrivacyPolicyResponse setPrivacyPolicy(@Valid @RequestBody SetPrivacyPolicyRequest request) {
        return storeService.setPrivacyPolicy(request);
    }
}
