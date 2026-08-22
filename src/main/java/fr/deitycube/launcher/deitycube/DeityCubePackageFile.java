package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeityCubePackageFile {

    private String path;
    private String sha256;
    private long size;

    public String getPath() {
        return path;
    }

    public String getSha256() {
        return sha256;
    }

    public long getSize() {
        return size;
    }
}
