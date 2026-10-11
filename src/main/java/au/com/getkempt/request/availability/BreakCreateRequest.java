package au.com.getkempt.request.availability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** Body for {@code POST /availability/{staffId}/breaks}. Empty {@code days} = every working day. */
public record BreakCreateRequest(
        @NotBlank @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") String startTime,
        @NotBlank @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") String endTime,
        String label,
        List<Integer> days) {
}
