package au.com.getkempt.responses.upload;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UploadConfirmResponse(String key, String url, Integer width, Integer height) {
}
