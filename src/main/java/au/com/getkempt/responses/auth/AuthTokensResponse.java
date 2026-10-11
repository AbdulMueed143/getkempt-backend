package au.com.getkempt.responses.auth;

/** Wire shape of the contract's {@code AuthTokens}. */
public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType) {

    public static AuthTokensResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthTokensResponse(accessToken, refreshToken, expiresInSeconds, "Bearer");
    }
}
