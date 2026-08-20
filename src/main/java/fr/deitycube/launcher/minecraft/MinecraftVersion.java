package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftVersion {

    private String id;
    private String type;

    @JsonProperty("releaseTime")
    private String releaseTime;

    private String sha1;
    private String url;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    public String getSha1() {
        return sha1;
    }

    public String getUrl() {
        return url;
    }
}
