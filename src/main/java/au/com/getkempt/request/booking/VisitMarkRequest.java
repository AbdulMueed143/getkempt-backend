package au.com.getkempt.request.booking;

import jakarta.validation.constraints.Size;

public record VisitMarkRequest(@Size(max = 500) String note) {
}
