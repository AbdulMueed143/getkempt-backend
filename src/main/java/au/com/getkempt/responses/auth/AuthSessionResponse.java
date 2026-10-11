package au.com.getkempt.responses.auth;

/** Wire shape of the contract's {@code AuthSession} — returned by signup/login/reset. */
public record AuthSessionResponse(UserResponse user, AuthTokensResponse tokens) {
}
