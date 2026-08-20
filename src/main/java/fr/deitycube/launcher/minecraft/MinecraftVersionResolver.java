package fr.deitycube.launcher.minecraft;

import java.io.IOException;
import java.nio.file.Path;

public final class MinecraftVersionResolver {

    private final MinecraftMetadataManager metadataManager;

    public MinecraftVersionResolver() {
        metadataManager =
                new MinecraftMetadataManager();
    }

    public MinecraftVersionMetadata load(
            Path versionJson
    ) throws IOException {

        return metadataManager.load(
                versionJson
        );
    }

    public MinecraftResolvedVersion loadResolved(
            Path versionJson,
            Path minecraftJar
    ) throws IOException {

        MinecraftVersionMetadata version =
                load(
                        versionJson
                );

        String parentId =
                version.getInheritsFrom();

        if (parentId == null
                || parentId.isBlank()) {

            return new MinecraftResolvedVersion(
                    version,
                    null,
                    minecraftJar
            );
        }

        Path versionsDirectory =
                versionJson
                        .getParent()
                        .getParent();

        Path parentJson =
                versionsDirectory
                        .resolve(parentId)
                        .resolve(
                                parentId + ".json"
                        );

        MinecraftVersionMetadata parent =
                load(
                        parentJson
                );

        return new MinecraftResolvedVersion(
                version,
                parent,
                minecraftJar
        );
    }
}