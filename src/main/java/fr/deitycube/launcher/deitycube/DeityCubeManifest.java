package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeityCubeManifest {

    @JsonProperty("modpack_version")
    private String modpackVersion;

    @JsonProperty("minecraft_version")
    private String minecraftVersion;

    @JsonProperty("neoforge_version")
    private String neoforgeVersion;

    private DeityCubeCommonFiles common;

    private Map<String, DeityCubePackage> profiles;

    // Manifeste du serveur test uniquement : adresse du serveur à rejoindre au lancement.
    private String server;

    public String getModpackVersion() {
        return modpackVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getNeoforgeVersion() {
        return neoforgeVersion;
    }

    public DeityCubeCommonFiles getCommon() {
        return common;
    }

    public Map<String, DeityCubePackage> getProfiles() {
        return profiles;
    }

    /** Adresse du serveur (hôte:port) à rejoindre directement, ou null. */
    public String getServer() {
        return server;
    }
}
