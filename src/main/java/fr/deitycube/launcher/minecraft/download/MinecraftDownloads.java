package fr.deitycube.launcher.minecraft.download;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftDownloads {

    private MinecraftDownload client;
    private MinecraftDownload server;

    public MinecraftDownload getClient() {
        return client;
    }

    public MinecraftDownload getServer() {
        return server;
    }
}
