package au.com.getkempt.request.loyalty;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProgramReorderRequest(@NotEmpty List<String> programIds) {
}
