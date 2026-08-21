package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeityCubePackage {

    private String version;
    private String name;
    private String filename;
    private String sha256;

    @JsonProperty("download_url")
    private String downloadUrl;

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getSha256() {
        return sha256;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
