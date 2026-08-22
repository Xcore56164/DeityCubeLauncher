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

        if (manifest.getVersion() == null || manifest.getVersion().isBlank()) {
            throw new IOException(
                    "Champ 'version' absent du manifeste de mise à jour du launcher."
            );
        }

        return manifest;
    }
}
