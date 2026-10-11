package au.com.getkempt.integrations.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Local push stub — logs the notification instead of dispatching it. */
@Component
@Slf4j
public class StubPushSender implements PushSender {

    @Override
    public Result send(String deviceToken, String title, String body, Map<String, String> data) {
        String id = "push_stub_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[stub-push] device={} title=\"{}\" -> {}", deviceToken, title, id);
        return new Result(id);
    }
}
