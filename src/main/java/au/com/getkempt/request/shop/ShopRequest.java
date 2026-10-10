package au.com.getkempt.request.shop;


import jakarta.validation.constraints.NotBlank;

/**
 * Placeholder request for the legacy {@code /api/v1/shop} stub. A record so Jackson can
 * deserialize it (the previous class had neither a no-arg constructor nor accessors).
 */
public record ShopRequest(
        @NotBlank String id
) {
}
