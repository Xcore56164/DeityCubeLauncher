package fr.deitycube.launcher.auth;

public class MinecraftProfile {

    private final String id;
    private final String name;
    private final String accessToken;

    public MinecraftProfile(
            String id,
            String name,
            String accessToken
    ) {
        this.id = id;
        this.name = name;
        this.accessToken = accessToken;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccessToken() {
        return accessToken;
    }
}