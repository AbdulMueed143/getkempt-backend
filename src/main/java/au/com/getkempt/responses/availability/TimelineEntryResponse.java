package au.com.getkempt.responses.availability;

import au.com.getkempt.enums.availability.TimelineStatus;

import java.time.LocalDate;

/** One entry of {@code GET /availability/{staffId}/timeline}. */
public record TimelineEntryResponse(LocalDate date, TimelineStatus status, int totalMinutes) {
}
