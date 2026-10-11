package au.com.getkempt.responses.booking;

import java.util.List;

public record BookingBulkResponse(List<String> succeeded, List<Failure> failed) {

    public record Failure(String id, String reason) {
    }
}
