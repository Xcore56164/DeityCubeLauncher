package fr.deitycube.launcher.auth;

public final class XstsToken {

    private final String token;
    private final String userHash;

    public XstsToken(
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
