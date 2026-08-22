package fr.deitycube.launcher.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LauncherUpdateTarget {

    @JsonProperty("installer_url")
    private String installerUrl;

    private String sha256;

    public String getInstallerUrl() {
        return installerUrl;
    }

    public String getSha256() {
        return sha256;
    }
}
