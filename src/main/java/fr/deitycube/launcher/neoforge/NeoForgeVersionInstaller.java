package fr.deitycube.launcher.neoforge;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NeoForgeVersionInstaller {

    private final Path gameDirectory;

    public NeoForgeVersionInstaller(
            Path gameDirectory
    ) {
        this.gameDirectory = gameDirectory;
    }

    public Path install(
            NeoForgeInstallerProfile profile
    ) throws IOException {

        JsonNode version =
                profile.getVersion();

        String versionId =
                version
                        .path("id")
                        .asText(null);

        if (versionId == null
                || versionId.isBlank()) {

            throw new IOException(
                    "L'identifiant de version NeoForge "
                            + "est absent de version.json."
            );
        }

        Path versionDirectory =
                gameDirectory
                        .resolve("versions")
                        .resolve(versionId);

        Files.createDirectories(
                versionDirectory
        );

        Path versionJson =
                versionDirectory.resolve(
                        versionId + ".json"
                );

        Files.writeString(
                versionJson,
                profile.getVersionJson()
        );

        return versionDirectory;
    }
}
