package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;
import java.util.Map;

public final class DeityCubeManifestReader {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private DeityCubeManifestReader() {
    }

    public static DeityCubeManifest read(
            String manifestUrl
    ) throws IOException {

        String manifestJson =
                HttpDownloader.downloadText(
                        manifestUrl
                );

        DeityCubeManifest manifest =
                OBJECT_MAPPER.readValue(
                        manifestJson,
                        DeityCubeManifest.class
                );

        validate(manifest);

        return manifest;
    }

    private static void validate(
            DeityCubeManifest manifest
    ) throws IOException {

        requireText(
                manifest.getMinecraftVersion(),
                "minecraft_version"
        );

        requireText(
                manifest.getNeoforgeVersion(),
                "neoforge_version"
        );

        requireText(
                manifest.getModpackVersion(),
                "modpack_version"
        );

        if (manifest.getPackages() == null
                || manifest.getPackages().isEmpty()) {

            throw new IOException(
                    "Champ 'package' absent ou vide "
                            + "dans le manifest DeityCube."
            );
        }

        for (Map.Entry<String, DeityCubePackage> entry :
                manifest.getPackages().entrySet()) {

            String profile =
                    entry.getKey();

            DeityCubePackage pack =
                    entry.getValue();

            if (pack == null) {

                throw new IOException(
                        "Profil '"
                                + profile
                                + "' vide dans le manifest DeityCube."
                );
            }

            requireText(
                    pack.getDownloadUrl(),
                    "package." + profile + ".download_url"
            );

            requireText(
                    pack.getSha256(),
                    "package." + profile + ".sha256"
            );

            requireText(
                    pack.getFilename(),
                    "package." + profile + ".filename"
            );
        }
    }

    private static void requireText(
            String value,
            String fieldName
    ) throws IOException {

        if (value == null || value.isBlank()) {

            throw new IOException(
                    "Champ '"
                            + fieldName
                            + "' absent du manifest DeityCube."
            );
        }
    }
}
