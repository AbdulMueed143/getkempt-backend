package au.com.getkempt.responses.availability;

import java.util.List;

/** Wire shape for {@code POST /availability/{staffId}/copy}. */
public record CopyScheduleResponse(List<String> appliedTo) {
}
