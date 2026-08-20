package fr.deitycube.launcher.auth;

import java.util.UUID;

public final class AuthenticationResult {

    private final AuthenticationMode mode;
    private final String username;
    private final UUID uuid;
    private final String accessToken;

    public AuthenticationResult(
            AuthenticationMode mode,
            String username,
            UUID uuid,
            String accessToken
    ) {
        this.mode = mode;
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
    }

    public AuthenticationMode getMode() {
        return mode;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
