package fr.deitycube.launcher.auth;

public class MinecraftToken {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;

    public MinecraftToken(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
