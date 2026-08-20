package fr.deitycube.launcher.neoforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NeoForgeInstaller {

    private static final String MINECRAFT_VERSION =
            "1.21.1";

    private static final String NEOFORGE_VERSION =
            "21.1.248";

    private static final String NEOFORGE_MAVEN_URL =
            "https://maven.neoforged.net/releases/"
                    + "net/neoforged/neoforge/"
                    + NEOFORGE_VERSION
                    + "/";

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private final Path gameDirectory;

    public NeoForgeInstaller(
            Path gameDirectory
    ) {
        this.gameDirectory =
                gameDirectory;
    }

    public Path downloadInstaller()
            throws IOException, InterruptedException {

        String fileName =
                "neoforge-"
                        + NEOFORGE_VERSION
                        + "-installer.jar";

        String url =
                NEOFORGE_MAVEN_URL
                        + fileName;

        Path destination =
                gameDirectory
                        .resolve("cache")
                        .resolve("neoforge")
                        .resolve(fileName);

        System.out.println();
        System.out.println(
                "================================="
        );
        System.out.println(
                "       NEOFORGE INSTALLER"
        );
        System.out.println(
                "================================="
        );

        System.out.println(
                "Minecraft : "
                        + MINECRAFT_VERSION
        );

        System.out.println(
                "NeoForge : "
                        + NEOFORGE_VERSION
        );

        if (Files.exists(destination)) {

            System.out.println(
                    "Installer déjà présent : "
                            + destination
            );

            return destination;
        }

        System.out.println(
                "Récupération du SHA-256 officiel..."
        );

        String expectedSha256 =
                downloadSha256(fileName);

        System.out.println(
                "SHA-256 officiel : "
                        + expectedSha256
        );

        HttpDownloader.downloadSha256(
                url,
                destination,
                expectedSha256
        );

        return destination;
    }

    private String downloadSha256(
            String fileName
    ) throws IOException {

        String hashUrl =
                NEOFORGE_MAVEN_URL
                        + fileName
                        + ".sha256";

        String sha256 =
                HttpDownloader.downloadText(
                        hashUrl
                );

        if (sha256.isBlank()) {
            throw new IOException(
                    "NeoForge a retourné un SHA-256 vide."
            );
        }

        return sha256;
    }

    public NeoForgeInstallerProfile readInstallerProfile(
            Path installer
    ) throws IOException {

        String installProfileJson = null;
        String versionJson = null;

        try (ZipFile zipFile =
                     new ZipFile(installer.toFile())) {

            ZipEntry installProfileEntry =
                    zipFile.getEntry(
                            "install_profile.json"
                    );

            ZipEntry versionEntry =
                    zipFile.getEntry(
                            "version.json"
                    );

            if (installProfileEntry == null) {

                throw new IOException(
                        "install_profile.json est absent "
                                + "de l'installer NeoForge."
                );
            }

            if (versionEntry == null) {

                throw new IOException(
                        "version.json est absent "
                                + "de l'installer NeoForge."
                );
            }

            installProfileJson =
                    readEntry(
                            zipFile,
                            installProfileEntry
                    );

            versionJson =
                    readEntry(
                            zipFile,
                            versionEntry
                    );
        }

        JsonNode installProfile =
                OBJECT_MAPPER.readTree(
                        installProfileJson
                );

        JsonNode version =
                OBJECT_MAPPER.readTree(
                        versionJson
                );

        return new NeoForgeInstallerProfile(
                installProfileJson,
                versionJson,
                installProfile,
                version
        );
    }

    private String readEntry(
            ZipFile zipFile,
            ZipEntry entry
    ) throws IOException {

        try (InputStream inputStream =
                     zipFile.getInputStream(entry)) {

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
