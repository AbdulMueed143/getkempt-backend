package au.com.getkempt.mappers;

import au.com.getkempt.entities.Session;
import au.com.getkempt.responses.auth.SessionResponse;
import org.springframework.stereotype.Component;

/** Maps {@link Session} → wire {@code SessionResponse}, flagging the caller's own session. */
@Component
public class SessionMapper {

    public SessionResponse toResponse(Session session, String currentSessionId) {
        return new SessionResponse(
                session.getId(),
                session.getDeviceName(),
                session.getIp(),
                session.getLocation(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getId().equals(currentSessionId));
    }
}
