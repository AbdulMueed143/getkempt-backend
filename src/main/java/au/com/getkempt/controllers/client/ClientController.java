package au.com.getkempt.controllers.client;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping('/api/v1/user')
public class ClientController {

    @PostMapping('/create')
    public ClientResponse createUser(final ClientRequest clientRequest) {

    }

}
