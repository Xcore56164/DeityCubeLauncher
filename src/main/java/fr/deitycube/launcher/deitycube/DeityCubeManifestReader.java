package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

public final class DeityCubeManifestReader {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    // Hôte (nom ou IPv4) et port facultatif : passé tel quel à --quickPlayMultiplayer.
    private static final Pattern SERVER_ADDRESS =
            Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?(?::\\d{1,5})?");

    private DeityCubeManifestReader() {
    }

    public static DeityCubeManifest read(
            String manifestUrl
    ) throws IOException {

        String manifestJson =
                HttpDownloader.downloadText(
                        manifestUrl
                );

        return parse(manifestJson);
    }

    public static DeityCubeManifest parse(
            String manifestJson
    ) throws IOException {

        DeityCubeManifest manifest =
                OBJECT_MAPPER.readValue(
                        manifestJson,
                        DeityCubeManifest.class
                );

        validate(manifest);

        return manifest;
    }

    /**
     * Manifeste du serveur test (renvoyé par test.php). Ses fichiers sont téléchargés avec le
     * jeton de session du joueur : ils doivent donc être servis en HTTPS par trustedHost
     * (deitycube.fr), jamais par un autre site.
     */
    public static DeityCubeManifest readTestManifest(
            JsonNode manifestJson,
            String trustedHost
    ) throws IOException {

        DeityCubeManifest manifest =
                OBJECT_MAPPER.treeToValue(
                        manifestJson,
                        DeityCubeManifest.class
                );

        validate(manifest);

        requireText(
                manifest.getServer(),
                "server"
        );

        if (!SERVER_ADDRESS.matcher(manifest.getServer()).matches()) {

            throw new IOException(
                    "Adresse de serveur invalide dans le manifest DeityCube : "
                            + manifest.getServer()
            );
        }

        requireTrustedHost(
                manifest.getCommon().getBaseUrl(),
                trustedHost,
                "common.base_url"
        );

        for (Map.Entry<String, DeityCubePackage> entry :
                manifest.getProfiles().entrySet()) {

            requireTrustedHost(
                    entry.getValue().getBaseUrl(),
                    trustedHost,
                    "profiles." + entry.getKey() + ".base_url"
            );
        }

        return manifest;
    }

    private static void requireTrustedHost(
            String url,
            String trustedHost,
            String fieldName
    ) throws IOException {

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            uri = null;
        }

        if (uri == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !uri.getHost().equalsIgnoreCase(trustedHost)) {

            throw new IOException(
                    "Champ '"
                            + fieldName
                            + "' invalide dans le manifest DeityCube "
                            + "(attendu : https://"
                            + trustedHost
                            + "/...) : "
                            + url
            );
        }
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
