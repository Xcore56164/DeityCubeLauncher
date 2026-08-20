package fr.deitycube.launcher.neoforge;

import com.fasterxml.jackson.databind.JsonNode;

public final class NeoForgeInstallerProfile {

    private final String installProfileJson;
    private final String versionJson;

    private final JsonNode installProfile;
    private final JsonNode version;

    public NeoForgeInstallerProfile(
            String installProfileJson,
            String versionJson,
            JsonNode installProfile,
            JsonNode version
    ) {
        this.installProfileJson = installProfileJson;
        this.versionJson = versionJson;
        this.installProfile = installProfile;
        this.version = version;
    }

    public String getInstallProfileJson() {
        return installProfileJson;
    }

    public String getVersionJson() {
        return versionJson;
    }

    public JsonNode getInstallProfile() {
        return installProfile;
    }

    public JsonNode getVersion() {
        return version;
    }
}
