package au.com.getkempt.responses.client;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Wire shape for {@code POST /clients/export}'s 202 response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientExportResponse(String jobId, String statusUrl, Instant expectedReadyAt) {
}
