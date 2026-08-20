package fr.deitycube.launcher.minecraft.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftAsset {

    private String hash;
    private long size;

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }
}
