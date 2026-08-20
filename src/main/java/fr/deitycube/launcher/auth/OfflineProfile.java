package fr.deitycube.launcher.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class OfflineProfile {

    private final String username;
    private final UUID uuid;

    public OfflineProfile(String username) {
        this.username = username;
        this.uuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String getName() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }
}
