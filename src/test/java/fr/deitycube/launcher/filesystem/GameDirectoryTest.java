package fr.deitycube.launcher.filesystem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameDirectoryTest {

    private static final Path ROOT = Path.of("DeityCube");

    @Test
    void mainProfilesShareGameDirectory() {

        assertEquals(ROOT.resolve("game"), GameDirectory.resolveGameDirectory(ROOT, "Max"));
        assertEquals(ROOT.resolve("game"), GameDirectory.resolveGameDirectory(ROOT, "light"));
        assertEquals(ROOT.resolve("game"), GameDirectory.resolveGameDirectory(ROOT, null));
    }

    @Test
    void testProfileHasItsOwnGameDirectory() {

        assertEquals(ROOT.resolve("game-test"), GameDirectory.resolveGameDirectory(ROOT, "Test"));
        assertEquals(ROOT.resolve("game-test"), GameDirectory.resolveGameDirectory(ROOT, "test"));
    }

    @Test
    void gameDirectoriesAreSiblings() {

        Path game = GameDirectory.resolveGameDirectory(ROOT, "Max");
        Path gameTest = GameDirectory.resolveGameDirectory(ROOT, "Test");

        // Le nettoyage d'un modpack reste dans son dossier : aucun des deux ne contient l'autre.
        assertFalse(gameTest.startsWith(game));
        assertFalse(game.startsWith(gameTest));
    }
}
