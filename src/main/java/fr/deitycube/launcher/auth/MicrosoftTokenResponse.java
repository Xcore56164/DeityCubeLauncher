package fr.deitycube.launcher.auth;

public final class MicrosoftTokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final String scope;
    private final long expiresIn;

    public MicrosoftTokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            String scope,
            long expiresIn
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
