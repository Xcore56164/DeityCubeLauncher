package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftLatest {

    private String release;
    private String snapshot;

    public String getRelease() {
        return release;
    }

    public String getSnapshot() {
        return snapshot;
    }
}
