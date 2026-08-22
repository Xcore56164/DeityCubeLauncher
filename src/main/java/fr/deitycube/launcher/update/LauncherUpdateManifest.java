package fr.deitycube.launcher.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LauncherUpdateManifest {

    private String version;

    @JsonProperty("installer_url")
    private String installerUrl;

    private String sha256;

    private String notes;

    public String getVersion() {
        return version;
    }

    public String getInstallerUrl() {
        return installerUrl;
    }

    public String getSha256() {
        return sha256;
    }

    public String getNotes() {
        return notes;
    }
}
