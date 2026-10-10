package au.com.getkempt.controllers.authentication;


import au.com.getkempt.enums.CallerType;
import au.com.getkempt.request.JWTRequest;
import au.com.getkempt.responses.jwt.JWTResponse;
import au.com.getkempt.services.authentication.JWTService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jwt")
public class JWTController {

    private final JWTService jwtService;

    @PostMapping("/staff/generate")
    public JWTResponse generateStaffJWT(@RequestBody @Valid final JWTRequest jwtRequest) {
        return new JWTResponse(jwtService.generateJwtResponse(jwtRequest, CallerType.STAFF));
    }

    @PostMapping("/client/generate")
    public JWTResponse generateClientJWT(@RequestBody @Valid final JWTRequest jwtRequest) {
        return new JWTResponse(jwtService.generateJwtResponse(jwtRequest, CallerType.CLIENT));
    }


//    @GetMapping("/staff/validate")
//    public StaffAuthenticationDetails validateStaffJwt() {
//
//    }
//
//    @GetMapping("/client/validate")
//    public ClientAuthenticationDetails validateClientJwt() {
//
//    }
//
//    @GetMapping("/staff/invalidate")
//    @GetMapping("/client/invalidate")


}
