package au.com.getkempt.responses.booking;

import java.util.List;

public record VisitBulkResponse(List<String> succeeded, List<Failure> failed) {

    public record Failure(String id, String reason) {
    }
}
