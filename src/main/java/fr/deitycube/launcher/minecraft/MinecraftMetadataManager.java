package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftMetadataManager {

    private final ObjectMapper objectMapper;

    public MinecraftMetadataManager() {
        this.objectMapper = new ObjectMapper();
    }

    public MinecraftVersionMetadata load(Path metadataPath)
            throws IOException {

        try (InputStream inputStream =
                     Files.newInputStream(metadataPath)) {

            return objectMapper.readValue(
                    inputStream,
                    MinecraftVersionMetadata.class
            );
        }
    }
}
