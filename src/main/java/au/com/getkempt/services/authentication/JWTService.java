package au.com.getkempt.services.authentication;


import au.com.getkempt.enums.CallerType;
import au.com.getkempt.request.JWTRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class JWTService {

    private final StaffAuthenticationService staffAuthenticationService;
    private final ClientAuthenticationService clientAuthenticationService;

    public String generateJwtResponse(final JWTRequest jwtRequest, final CallerType callerType) {
        return "testing";
    }


    // TODO(phase-2 auth): implement real validation / revocation against the session store.
    public String validateJwt(final String jwt, final CallerType callerType) {
        return null;
    }

    public String invalidateJwt(final String jwt, final CallerType callerType) {
        return null;
    }


}
