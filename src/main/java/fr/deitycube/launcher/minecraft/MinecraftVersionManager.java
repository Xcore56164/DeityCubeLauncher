package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftVersionManager {

    private final ObjectMapper objectMapper;

    public MinecraftVersionManager() {
        this.objectMapper = new ObjectMapper();
    }

    public MinecraftVersion findTargetVersion(Path manifestPath)
            throws IOException {

        try (var inputStream = Files.newInputStream(manifestPath)) {

            MinecraftVersionManifest manifest =
                    objectMapper.readValue(
                            inputStream,
                            MinecraftVersionManifest.class
                    );

            return manifest.getVersions()
                    .stream()
                    .filter(version ->
                            LauncherConfig.MINECRAFT_VERSION.equals(
                                    version.getId()
                            )
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new IOException(
                                    "Minecraft "
                                            + LauncherConfig.MINECRAFT_VERSION
                                            + " est introuvable dans le manifeste Mojang."
                            )
                    );
        }
    }

    public Path downloadMetadata(
            MinecraftVersion version,
            Path destination
    ) throws IOException {

        HttpDownloader.downloadSha1(
                version.getUrl(),
                destination
        );

        return destination;
    }
}
