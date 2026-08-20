package fr.deitycube.launcher.auth;

public final class XboxLiveToken {

    private final String token;
    private final String userHash;

    public XboxLiveToken(
            String token,
            String userHash
    ) {
        this.token = token;
        this.userHash = userHash;
    }

    public String getToken() {
        return token;
    }

    public String getUserHash() {
        return userHash;
    }
}
