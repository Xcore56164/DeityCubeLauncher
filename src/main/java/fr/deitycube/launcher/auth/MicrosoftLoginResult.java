package fr.deitycube.launcher.auth;

public final class MicrosoftLoginResult {

    private final AuthenticationResult authentication;
    private final String refreshToken;

    public MicrosoftLoginResult(
            AuthenticationResult authentication,
            String refreshToken
    ) {
        this.authentication = authentication;
        this.refreshToken = refreshToken;
    }

    public AuthenticationResult getAuthentication() {
        return authentication;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
