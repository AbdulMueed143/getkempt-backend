package au.com.getkempt.integrations.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Local email stub — logs the message instead of dispatching it. */
@Component
@Slf4j
public class StubEmailSender implements EmailSender {

    @Override
    public Result send(String toEmail, String subject, String htmlBody) {
        String id = "email_stub_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[stub-email] to={} subject=\"{}\" -> {}", toEmail, subject, id);
        return new Result(id);
    }
}
