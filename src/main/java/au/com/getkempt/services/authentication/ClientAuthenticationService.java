package au.com.getkempt.services.authentication;

import au.com.getkempt.repositories.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ClientAuthenticationService {

    private ClientRepository repository;

    //registering the client
    public void register() {

    }

}
