package au.com.getkempt.controllers.authentication;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jwt")
public class JWTController {

    private final JwtService jwtService;

    public JwtResponse generateStaffJWT() {

    }

    public JwtResponse generateClientJWT() {

    }

}
