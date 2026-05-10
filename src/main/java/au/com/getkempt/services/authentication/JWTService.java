package au.com.getkempt.services.authentication;


import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class JWTService {

    private final StaffAuthenticationService staffAuthenticationService;
    private final ClientAuthenticationService clientAuthenticationService;

}
