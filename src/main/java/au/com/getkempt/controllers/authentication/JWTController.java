package au.com.getkempt.controllers.authentication;


import au.com.getkempt.responses.jwt.JWTResponse;
import au.com.getkempt.services.authentication.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jwt")
public class JWTController {

    private final JWTService jwtService;

    public JWTResponse generateStaffJWT() {

    }

    public JWTResponse generateClientJWT() {

    }

}
