package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeityCubeManifestReaderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String HOST = "deitycube.fr";

    /** Manifeste test tel que renvoyé par test.php ; server et base_url remplaçables. */
    static String testManifest(String server, String commonBaseUrl, String testBaseUrl) {

        return """
                {
                  "modpack_version": "test-20261017",
                  "minecraft_version": "1.21.1",
                  "neoforge_version": "21.1.200",
                  %s
                  "common": {
                    "base_url": "%s",
                    "files": [
                      {"path": "config/deitycube-common.toml", "sha256": "aa", "size": 2}
                    ]
                  },
                  "profiles": {
                    "test": {
                      "name": "Test",
                      "version": "test-20261017",
                      "base_url": "%s",
                      "files": [
                        {"path": "mods/deitycube-0.2.0.jar", "sha256": "bb", "size": 2}
                      ]
                    }
                  }
                }
                """.formatted(
                server == null ? "" : "\"server\": \"" + server + "\",",
                commonBaseUrl,
                testBaseUrl
        );
    }

    private static JsonNode json(String text) throws IOException {
        return OBJECT_MAPPER.readTree(text);
    }

    @Test
    void readsTestManifestAndServer() throws IOException {

        DeityCubeManifest manifest = DeityCubeManifestReader.readTestManifest(
                json(testManifest(
                        "deitycube.fr:25566",
                        "https://deitycube.fr/downloads/test/common",
                        "https://deitycube.fr/downloads/test/test"
                )),
                HOST
        );

        assertEquals("deitycube.fr:25566", manifest.getServer());
        assertEquals("1.21.1", manifest.getMinecraftVersion());
        assertEquals("21.1.200", manifest.getNeoforgeVersion());
        assertTrue(manifest.getProfiles().containsKey("test"));
        assertEquals(
                "mods/deitycube-0.2.0.jar",
                manifest.getProfiles().get("test").getFiles().get(0).getPath()
        );
    }

    @Test
    void mainManifestHasNoServer() throws IOException {

        DeityCubeManifest manifest = DeityCubeManifestReader.parse(testManifest(
                null,
                "https://deitycube.fr/downloads/common",
                "https://deitycube.fr/downloads/max"
        ));

        assertNull(manifest.getServer());
    }

    @Test
    void testManifestRequiresValidServer() {

        assertThrows(IOException.class, () -> DeityCubeManifestReader.readTestManifest(
                json(testManifest(
                        null,
                        "https://deitycube.fr/downloads/test/common",
                        "https://deitycube.fr/downloads/test/test"
                )),
                HOST
        ));

        assertThrows(IOException.class, () -> DeityCubeManifestReader.readTestManifest(
                json(testManifest(
                        "--demo",
                        "https://deitycube.fr/downloads/test/common",
                        "https://deitycube.fr/downloads/test/test"
                )),
                HOST
        ));
    }

    @Test
    void testFilesMustComeFromTheSite() {

        // Le jeton de session accompagne ces téléchargements : jamais vers un autre site,
        // ni en clair.
        assertThrows(IOException.class, () -> DeityCubeManifestReader.readTestManifest(
                json(testManifest(
                        "deitycube.fr:25566",
                        "https://deitycube.fr/downloads/test/common",
                        "https://exemple.com/downloads/test/test"
                )),
                HOST
        ));

        assertThrows(IOException.class, () -> DeityCubeManifestReader.readTestManifest(
                json(testManifest(
                        "deitycube.fr:25566",
                        "http://deitycube.fr/downloads/test/common",
                        "https://deitycube.fr/downloads/test/test"
                )),
                HOST
        ));
    }

    /** Manifeste tel que l'écrit deitycube-mod sur le VPS : profil "test" sans fichiers propres. */
    private static String siteTestManifest(String commonFiles) {

        return """
                {
                  "modpack_version": "test-20261017-120000",
                  "minecraft_version": "1.21.1",
                  "neoforge_version": "21.1.216",
                  "common": {
                    "base_url": "https://deitycube.fr/downloads/test/common/",
                    "files": [%s]
                  },
                  "profiles": {
                    "test": {
                      "name": "Serveur test",
                      "version": "test-20261017-120000",
                      "base_url": "https://deitycube.fr/downloads/test/profile/",
                      "files": []
                    }
                  },
                  "server": "deitycube.fr:25566"
                }
                """.formatted(commonFiles);
    }

    @Test
    void acceptsSiteTestManifestWithEmptyTestProfile() throws IOException {

        DeityCubeManifest manifest = DeityCubeManifestReader.readTestManifest(
                json(siteTestManifest(
                        "{\"path\": \"mods/deitycube-0.2.0.jar\", \"sha256\": \"aa\", \"size\": 2}"
                )),
                HOST
        );

        assertEquals(1, manifest.getCommon().getFiles().size());
        assertTrue(manifest.getProfiles().get("test").getFiles().isEmpty());
        assertEquals("deitycube.fr:25566", manifest.getServer());
    }

    @Test
    void acceptsEmptyTestModpack() throws IOException {

        // Plus aucun mod en test : NeoForge seul.
        DeityCubeManifest manifest = DeityCubeManifestReader.readTestManifest(
                json(siteTestManifest("")),
                HOST
        );

        assertTrue(manifest.getCommon().getFiles().isEmpty());
    }

    @Test
    void mainManifestStillRequiresFiles() {

        assertThrows(IOException.class, () -> DeityCubeManifestReader.parse(
                siteTestManifest("")
        ));
    }
}
