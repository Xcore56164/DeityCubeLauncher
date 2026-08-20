package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftVersionManifest {

    private MinecraftLatest latest;
    private List<MinecraftVersion> versions;

    public MinecraftLatest getLatest() {
        return latest;
    }

    public List<MinecraftVersion> getVersions() {
        return versions;
    }
}
