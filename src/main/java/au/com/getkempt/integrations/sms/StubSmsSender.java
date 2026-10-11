package au.com.getkempt.integrations.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Local SMS stub — logs the message and estimates segments (160 chars each). */
@Component
@Slf4j
public class StubSmsSender implements SmsSender {

    @Override
    public Result send(String toPhoneE164, String body) {
        int segments = body == null || body.isEmpty() ? 1 : (int) Math.ceil(body.length() / 160.0);
        String id = "sms_stub_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[stub-sms] to={} segments={} body=\"{}\" -> {}", toPhoneE164, segments, body, id);
        return new Result(id, segments);
    }
}
