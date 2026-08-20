package fr.deitycube.launcher.minecraft.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftAssetIndex {

    private Map<String, MinecraftAsset> objects;

    public Map<String, MinecraftAsset> getObjects() {
        return objects;
    }
}
