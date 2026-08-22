package fr.deitycube.launcher.update;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;

public final class LauncherUpdateManifestReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LauncherUpdateManifestReader() {
    }

    public static LauncherUpdateManifest read(String manifestUrl) throws IOException {

        String manifestJson = HttpDownloader.downloadText(manifestUrl);

        LauncherUpdateManifest manifest =
                OBJECT_MAPPER.readValue(manifestJson, LauncherUpdateManifest.class);

        validate(manifest);

        return manifest;
    }

    private static void validate(LauncherUpdateManifest manifest) throws IOException {

        requireText(manifest.getVersion(), "version");
        requireText(manifest.getInstallerUrl(), "installer_url");
        requireText(manifest.getSha256(), "sha256");

        if (!manifest.getInstallerUrl().startsWith("https://")) {
            throw new IOException(
                    "Le champ 'installer_url' du manifeste de mise à jour doit être en HTTPS."
            );
        }
    }

    private static void requireText(String value, String fieldName) throws IOException {

        if (value == null || value.isBlank()) {
            throw new IOException(
                    "Champ '" + fieldName + "' absent du manifeste de mise à jour du launcher."
            );
        }
    }
}
