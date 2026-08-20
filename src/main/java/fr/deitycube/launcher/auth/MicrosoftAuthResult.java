package fr.deitycube.launcher.auth;

public final class MicrosoftAuthResult {

    private final String authorizationCode;
    private final String codeVerifier;
    private final String redirectUri;

    public MicrosoftAuthResult(
            String authorizationCode,
            String codeVerifier,
            String redirectUri
    ) {
        this.authorizationCode = authorizationCode;
        this.codeVerifier = codeVerifier;
        this.redirectUri = redirectUri;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
