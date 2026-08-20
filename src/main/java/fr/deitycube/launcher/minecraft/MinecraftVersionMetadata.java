package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.deitycube.launcher.minecraft.assets.MinecraftAssetIndexInfo;
import fr.deitycube.launcher.minecraft.download.MinecraftDownloads;
import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftVersionMetadata {

    private String id;
    private String type;

    @JsonProperty("mainClass")
    private String mainClass;

    private String assets;

    private MinecraftDownloads downloads;

    private List<MinecraftLibrary> libraries;

    private MinecraftAssetIndexInfo assetIndex;

    private String inheritsFrom;

    private String jar;

    private MinecraftArguments arguments;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getAssets() {
        return assets;
    }

    public MinecraftDownloads getDownloads() {
        return downloads;
    }

    public List<MinecraftLibrary> getLibraries() {
        return libraries;
    }

    public MinecraftAssetIndexInfo getAssetIndex() {
        return assetIndex;
    }

    public String getInheritsFrom() {
        return inheritsFrom;
    }

    public MinecraftArguments getArguments() {
        return arguments;
    }

    public String getJar() {
        return jar;
    }
}
