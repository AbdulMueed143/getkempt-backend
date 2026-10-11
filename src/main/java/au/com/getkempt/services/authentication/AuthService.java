package au.com.getkempt.services.authentication;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.UnauthorizedException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.security.Tokens;
import au.com.getkempt.entities.User;
import au.com.getkempt.entities.store.StoreAddress;
import au.com.getkempt.entities.store.StoreGooglePlace;
import au.com.getkempt.entities.store.StoreGooglePlacePhoto;
import au.com.getkempt.entities.store.StoreProfile;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.enums.UserRole;
import au.com.getkempt.enums.store.BookingWindowUnit;
import au.com.getkempt.enums.store.BusinessType;
import au.com.getkempt.enums.store.DepositType;
import au.com.getkempt.enums.store.FeeType;
import au.com.getkempt.enums.store.InStoreMethod;
import au.com.getkempt.enums.store.PaymentMode;
import au.com.getkempt.integrations.places.PlacesLookupPort;
import au.com.getkempt.mappers.UserMapper;
import au.com.getkempt.repositories.store.StoreProfileRepository;
import au.com.getkempt.repositories.store.StoreSettingsRepository;
import au.com.getkempt.repositories.user.UserRepository;
import au.com.getkempt.services.outbox.OutboxService;
import au.com.getkempt.request.auth.ForgotPasswordRequest;
import au.com.getkempt.request.auth.LoginRequest;
import au.com.getkempt.request.auth.ResetPasswordRequest;
import au.com.getkempt.request.auth.SignupRequest;
import au.com.getkempt.request.auth.VerifyEmailRequest;
import au.com.getkempt.responses.auth.AuthSessionResponse;
import au.com.getkempt.responses.auth.AuthTokensResponse;
import au.com.getkempt.responses.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Auth orchestration: signup (provisions the tenant), login, refresh, logout, and the
 * password-reset / email-verification flows. Passwords are argon2id-hashed; reset and
 * verification tokens are random and stored only as hashes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final long RESET_TOKEN_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final StoreProfileRepository storeProfileRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;
    private final PlacesLookupPort placesLookupPort;
    private final UserMapper userMapper;
    private final SignupBootstrapService signupBootstrapService;

    /**
     * Provision a new tenant: a {@link StoreProfile} + {@link StoreSettings} + the owner
     * {@link User}. The owner is written last so a duplicate-email race fails before any
     * orphaned shop is referenced (the user's unique email index is the commit point).
     *
     * <p>In production this runs inside a Mongo multi-document transaction (see
     * {@code MongoTransactionConfig}); it is intentionally not annotated here so it also
     * works against a standalone (non-replica-set) dev Mongo.
     */
    public AuthSessionResponse signup(SignupRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        String shopId = IdGenerator.generate(IdPrefix.SHOP);
        UserRole role = UserRole.from(request.role());

        PlaceLookup placeLookup = lookUpPlace(request.placeId());

        StoreProfile profile = StoreProfile.builder()
                .id(shopId)
                .slug(uniqueSlug(request.businessName()))
                .shopName(request.businessName())
                .businessType(role == UserRole.SALON_OWNER ? BusinessType.HAIR_SALON : BusinessType.BARBERSHOP)
                .email(email)
                .phone(placeLookup.phone() == null ? "" : placeLookup.phone())
                .website(placeLookup.website())
                .timezone("Australia/Melbourne") // Places doesn't return a timezone; deriving one from lat/lng would need a separate Time Zone API call — left as follow-up
                .address(placeLookup.address())
                .googlePlace(placeLookup.googlePlace())
                .build();

        //Store profile repository setup ...
        storeProfileRepository.save(profile);
        storeSettingsRepository.save(defaultSettings(shopId));

        String verificationToken = Tokens.randomToken();
        User user;
        try {
            user = userRepository.save(User.builder()
                    .id(IdGenerator.generate(IdPrefix.USER))
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .role(role)
                    .businessName(request.businessName())
                    .shopId(shopId)
                    .emailVerificationTokenHash(Tokens.sha256(verificationToken))
                    .build());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ConflictException("An account with this email already exists");
        }

        sendVerificationEmail(user, verificationToken);
        signupBootstrapService.bootstrap(user, role);

        AuthTokensResponse tokens = sessionService.issue(user, httpRequest);
        return new AuthSessionResponse(userMapper.toResponse(user), tokens);
    }

    public AuthSessionResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        AuthTokensResponse tokens = sessionService.issue(user, httpRequest);
        return new AuthSessionResponse(userMapper.toResponse(user), tokens);
    }

    public AuthTokensResponse refresh(String refreshToken, HttpServletRequest httpRequest) {
        return sessionService.rotate(refreshToken, httpRequest);
    }

    public void logout(String sessionId) {
        sessionService.revoke(sessionId);
    }

    /** Always succeeds (204) regardless of whether the email exists — no account enumeration. */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String resetToken = Tokens.randomToken();
            user.setPasswordResetTokenHash(Tokens.sha256(resetToken));
            user.setPasswordResetExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);
            sendResetEmail(user, resetToken);
        });
    }

    public AuthSessionResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByPasswordResetTokenHash(Tokens.sha256(request.token()))
                .orElseThrow(() -> new UnauthorizedException("Reset token is invalid"));

        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new UnprocessableEntityException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        // Resetting a password logs the user out everywhere, then signs them back in fresh.
        sessionService.revokeOtherSessions(user.getId(), null);
        AuthTokensResponse tokens = sessionService.issue(user, httpRequest);
        return new AuthSessionResponse(userMapper.toResponse(user), tokens);
    }

    public UserResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailVerificationTokenHash(Tokens.sha256(request.token()))
                .orElseThrow(() -> new UnauthorizedException("Verification token is invalid"));

        user.setEmailVerifiedAt(Instant.now());
        user.setEmailVerificationTokenHash(null);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public void resendVerification(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        if (user.isEmailVerified()) {
            return;
        }
        String verificationToken = Tokens.randomToken();
        user.setEmailVerificationTokenHash(Tokens.sha256(verificationToken));
        userRepository.save(user);
        sendVerificationEmail(user, verificationToken);
    }

    // --- helpers ---------------------------------------------------------------

    private StoreSettings defaultSettings(String shopId) {
        return StoreSettings.builder()
                .id(shopId)
                .bookingWindowValue(30)
                .bookingWindowUnit(BookingWindowUnit.DAYS)
                .slotInterval(15)
                .paymentMode(PaymentMode.IN_STORE)
                .depositType(DepositType.PERCENTAGE)
                .depositValue(0.0)
                .inStoreMethods(List.of(InStoreMethod.CARD, InStoreMethod.CASH))
                .stripeConnected(false)
                .noShowFeeType(FeeType.NONE)
                .noShowFeeValue(0.0)
                .autoCharge(false)
                .build();
    }

    private record PlaceLookup(StoreAddress address, StoreGooglePlace googlePlace, String phone, String website) {
    }

    /**
     * Resolves the frontend-supplied {@code placeId} via {@link PlacesLookupPort},
     * synchronously, but never lets a lookup failure block signup — an invalid
     * placeId, a Places API outage, or a network hiccup should never be the
     * reason someone can't create an account. On failure, the raw placeId is
     * still retained (so it can be retried/re-synced later) but every other
     * field is left blank, exactly as signup behaved before this integration
     * existed.
     */
    private PlaceLookup lookUpPlace(String placeId) {
        try {
            PlacesLookupPort.PlaceDetails place = placesLookupPort.lookup(placeId);
            StoreAddress address = StoreAddress.builder()
                    .line1(nullToEmpty(place.line1()))
                    .suburb(nullToEmpty(place.suburb()))
                    .state(nullToEmpty(place.state()))
                    .postcode(nullToEmpty(place.postcode()))
                    .country(place.country() == null ? "AU" : place.country())
                    .latitude(place.latitude())
                    .longitude(place.longitude())
                    .placeId(placeId)
                    .build();
            StoreGooglePlace googlePlace = StoreGooglePlace.builder()
                    .placeId(placeId)
                    .rating(place.rating())
                    .userRatingsTotal(place.userRatingsTotal())
                    .businessStatus(place.businessStatus())
                    .googleMapsUri(place.googleMapsUri())
                    .photos(place.photos().stream()
                            .map(p -> StoreGooglePlacePhoto.builder()
                                    .photoReference(p.photoReference()).widthPx(p.widthPx()).heightPx(p.heightPx())
                                    .build())
                            .toList())
                    .lastSyncedAt(Instant.now())
                    .build();
            return new PlaceLookup(address, googlePlace, place.formattedPhoneNumber(), place.websiteUri());
        } catch (RuntimeException e) {
            log.warn("[signup] Google Places lookup failed for placeId={} — proceeding with blank enrichment: {}", placeId, e.getMessage());
            StoreAddress fallback = StoreAddress.builder()
                    .line1("").suburb("").state("").postcode("").country("AU").placeId(placeId).build();
            return new PlaceLookup(fallback, null, null, null);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String uniqueSlug(String businessName) {
        String base = businessName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (base.isBlank()) {
            base = "shop";
        }
        String slug = base;
        while (storeProfileRepository.existsBySlug(slug)) {
            slug = base + "-" + IdGenerator.generate(IdPrefix.SHOP).substring(4, 10);
        }
        return slug;
    }

    /**
     * Queued via {@link OutboxService} rather than sent inline — see its class
     * javadoc. Signup itself is intentionally not wrapped in a Mongo transaction
     * (see this class's javadoc), so this enqueue has the same non-atomicity
     * with the user-creation write that the old direct {@code emailSender.send()}
     * call had; the win here is that a slow/down email provider can no longer
     * add latency to — or, if it throws, break — the signup response itself.
     */
    private void sendVerificationEmail(User user, String token) {
        String link = "https://app.getkempt.co/verify-email?token=" + token;
        outboxService.enqueueEmail(user.getShopId(), user.getEmail(), "Verify your GetKempt email",
                "<p>Welcome to GetKempt! Confirm your email: <a href=\"" + link + "\">Verify</a></p>",
                "auth.verify_email", user.getId());
    }

    private void sendResetEmail(User user, String token) {
        String link = "https://app.getkempt.co/reset-password?token=" + token;
        outboxService.enqueueEmail(user.getShopId(), user.getEmail(), "Reset your GetKempt password",
                "<p>Reset your password: <a href=\"" + link + "\">Reset</a> (valid for 1 hour)</p>",
                "auth.password_reset", user.getId());
    }
}
