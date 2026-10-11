package au.com.getkempt.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of the contract's {@code ImageRef}. The {@code url} is resolved per-request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageRefResponse(String key, String url, Integer width, Integer height) {
}
