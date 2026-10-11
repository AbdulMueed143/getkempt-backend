package au.com.getkempt.responses.booking;

import java.time.Instant;
import java.util.List;

public record ConflictCheckResponse(boolean available, List<Conflict> conflicts) {

    public record Conflict(String kind, Instant startAt, Instant endAt, String label) {
    }
}
