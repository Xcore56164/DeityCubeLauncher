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

        if (manifest.getCommon() == null) {

            throw new IOException(
                    "Champ 'common' absent du manifest DeityCube."
            );
        }

        requireText(
                manifest.getCommon().getBaseUrl(),
                "common.base_url"
        );

        if (manifest.getCommon().getFiles() == null
                || manifest.getCommon().getFiles().isEmpty()) {

            throw new IOException(
                    "Champ 'common.files' absent ou vide "
                            + "dans le manifest DeityCube."
            );
        }

        for (DeityCubePackageFile file : manifest.getCommon().getFiles()) {

            validateFile("common", file);
        }

        if (manifest.getProfiles() == null
                || manifest.getProfiles().isEmpty()) {

            throw new IOException(
                    "Champ 'profiles' absent ou vide "
                            + "dans le manifest DeityCube."
            );
        }

        for (Map.Entry<String, DeityCubePackage> entry :
                manifest.getProfiles().entrySet()) {

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
                    pack.getBaseUrl(),
                    "profiles." + profile + ".base_url"
            );

            if (pack.getFiles() == null
                    || pack.getFiles().isEmpty()) {

                throw new IOException(
                        "Champ 'profiles."
                                + profile
                                + ".files' absent ou vide "
                                + "dans le manifest DeityCube."
                );
            }

            for (DeityCubePackageFile file : pack.getFiles()) {

                validateFile("profiles." + profile, file);
            }
        }
    }

    private static void validateFile(
            String section,
            DeityCubePackageFile file
    ) throws IOException {

        if (file == null) {

            throw new IOException(
                    "Entrée de fichier vide dans "
                            + section
                            + ".files."
            );
        }

        requireText(
                file.getPath(),
                section + ".files[].path"
        );

        requireText(
                file.getSha256(),
                section + ".files[].sha256"
        );

        if (!isSafeRelativePath(file.getPath())) {

            throw new IOException(
                    "Chemin de fichier invalide dans "
                            + section
                            + ".files : "
                            + file.getPath()
            );
        }
    }

    private static boolean isSafeRelativePath(
            String path
    ) {

        if (path.startsWith("/")
                || path.startsWith("\\")
                || path.contains("\\")
                || path.contains(":")) {

            return false;
        }

        for (String segment : path.split("/")) {

            if (segment.isBlank()
                    || segment.equals("..")
                    || segment.equals(".")) {

                return false;
            }
        }

        return true;
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
