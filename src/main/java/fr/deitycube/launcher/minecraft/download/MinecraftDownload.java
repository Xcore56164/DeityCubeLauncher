package fr.deitycube.launcher.minecraft.download;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftDownload {

    private String sha1;
    private long size;
    private String url;

    public String getSha1() {
        return sha1;
    }

    public long getSize() {
        return size;
    }

    public String getUrl() {
        return url;
    }
}
