package fr.deitycube.launcher.auth;

public final class MicrosoftAuthConfig {

    private MicrosoftAuthConfig() {
    }

    public static final String CLIENT_ID =
            "f2511c1d-8bc3-4ede-9189-4c6a9763460e";

    public static final String AUTHORIZATION_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";

    public static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    public static final String SCOPE =
            "XboxLive.signin offline_access";
}
