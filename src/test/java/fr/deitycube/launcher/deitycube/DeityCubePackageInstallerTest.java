package fr.deitycube.launcher.deitycube;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeityCubePackageInstallerTest {

    @TempDir
    Path root;

    private static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void pruningTestModpackNeverTouchesMainGameDirectory() throws IOException {

        Path game = root.resolve("game");
        Path gameTest = root.resolve("game-test");

        // Modpack principal déjà installé.
        Path mainMod = write(game.resolve("mods/deitycube-0.1.0.jar"), "principal");
        Path mainConfig = write(game.resolve("config/deitycube-common.toml"), "principal");

        // Profil Test : fichiers déjà à jour (aucun téléchargement), plus un mod retiré du test.
        Path testConfig = write(gameTest.resolve("config/deitycube-common.toml"), "cc");
        Path testMod = write(gameTest.resolve("mods/deitycube-0.2.0.jar"), "mm");
        Path removedMod = write(gameTest.resolve("mods/ancien-mod.jar"), "ancien");

        String json = DeityCubeManifestReaderTest.testManifest(
                        "deitycube.fr:25566",
                        "https://deitycube.fr/downloads/test/common",
                        "https://deitycube.fr/downloads/test/test"
                )
                .replace("\"aa\"", "\"" + HashUtils.sha256(testConfig) + "\"")
                .replace("\"bb\"", "\"" + HashUtils.sha256(testMod) + "\"");

        DeityCubeManifest manifest = DeityCubeManifestReader.parse(json);

        new DeityCubePackageInstaller(
                gameTest,
                Map.of("Authorization", "Bearer jeton")
        ).install(manifest, manifest.getProfiles().get("test"));

        assertTrue(Files.isRegularFile(testConfig));
        assertTrue(Files.isRegularFile(testMod));
        assertFalse(Files.exists(removedMod));

        assertTrue(Files.isRegularFile(mainMod));
        assertTrue(Files.isRegularFile(mainConfig));
    }

    @Test
    void lastModRemovedFromTestIsDeleted() throws IOException {

        Path gameTest = root.resolve("game-test");
        Path removedMod = write(gameTest.resolve("mods/deitycube-0.2.0.jar"), "ancien");
        Path options = write(gameTest.resolve("options.txt"), "réglages du joueur");

        // Plus aucun mod en test : manifeste généré par le site avec des listes vides.
        DeityCubeManifest manifest = DeityCubeManifestReader.readTestManifest(
                new ObjectMapper().readTree("""
                        {
                          "modpack_version": "test-20261017-130000",
                          "minecraft_version": "1.21.1",
                          "neoforge_version": "21.1.216",
                          "common": {"base_url": "https://deitycube.fr/downloads/test/common/", "files": []},
                          "profiles": {"test": {"name": "Serveur test", "version": "test-20261017-130000",
                                       "base_url": "https://deitycube.fr/downloads/test/profile/", "files": []}},
                          "server": "deitycube.fr:25566"
                        }
                        """),
                "deitycube.fr"
        );

        new DeityCubePackageInstaller(gameTest, Map.of())
                .install(manifest, manifest.getProfiles().get("test"));

        assertFalse(Files.exists(removedMod));
        assertTrue(Files.isRegularFile(options));
    }

    @Test
    void testProfileInstallsInGameTest() {

        DeityCubeGameProfile profile = DeityCubeGameProfile.test(new DeityCubeManifest(), "jeton");

        assertEquals(GameDirectory.getTestGameDirectory(), profile.getGameDirectory());
        assertEquals("test", profile.getModpackProfile());
        assertEquals("Bearer jeton", profile.getRequestHeaders().get("Authorization"));
    }
}
