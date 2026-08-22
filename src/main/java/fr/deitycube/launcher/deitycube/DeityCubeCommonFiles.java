package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeityCubeCommonFiles {

    @JsonProperty("base_url")
    private String baseUrl;

    private List<DeityCubePackageFile> files;

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<DeityCubePackageFile> getFiles() {
        return files;
    }
}
