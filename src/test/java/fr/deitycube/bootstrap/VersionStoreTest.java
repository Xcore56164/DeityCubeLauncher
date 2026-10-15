package fr.deitycube.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionStoreTest {

    @TempDir
    Path tmp;

    private VersionStore store;

    @BeforeEach
    void setUp() {
        store = new VersionStore(tmp.resolve("launcher"));
    }

    @Test
    void startsEmpty() {

        assertTrue(store.current().isEmpty());
        assertTrue(store.intactCurrent().isEmpty());
    }

    @Test
    void activatingMakesTheVersionCurrentAndKeepsTheOldOneAsFallback() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));
        store.activate("1.1.0", TestSupport.stagedPayload(tmp.resolve("b"), "1.1.0"));

        assertEquals(Optional.of("1.1.0"), store.current());
        assertEquals(Optional.of("1.0.0"), store.previous());
        assertTrue(store.intactCurrent().isPresent());
        assertTrue(Files.isRegularFile(store.versionDirectory("1.0.0").resolve("lib/launcher.jar")));
    }

    @Test
    void aMissingFileMakesTheActiveVersionUnusableSoItGetsReinstalled() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));

        // Un antivirus a mis un jar en quarantaine après coup.
        Files.delete(store.versionDirectory("1.0.0").resolve("lib/launcher.jar"));

        assertTrue(store.intactCurrent().isEmpty());
    }

    @Test
    void reinstallingTheSameVersionReplacesItsContent() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));

        Files.delete(store.versionDirectory("1.0.0").resolve("lib/launcher.jar"));

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("b"), "1.0.0"));

        assertTrue(store.intactCurrent().isPresent());
        assertTrue(store.previous().isEmpty());
    }

    @Test
    void rollbackRestoresThePreviousVersionAndBlocksTheFailedOne() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));
        store.activate("1.1.0", TestSupport.stagedPayload(tmp.resolve("b"), "1.1.0"));

        Optional<String> restored = store.rollback("1.1.0");

        assertEquals(Optional.of("1.0.0"), restored);
        assertEquals(Optional.of("1.0.0"), store.current());
        assertEquals(Optional.of("1.1.0"), store.blocked());
    }

    @Test
    void rollbackWithoutAFallbackStillBlocksTheFailedVersion() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));

        assertTrue(store.rollback("1.0.0").isEmpty());
        assertEquals(Optional.of("1.0.0"), store.blocked());
    }

    @Test
    void aNewerActivationLiftsTheBlock() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));
        store.activate("1.1.0", TestSupport.stagedPayload(tmp.resolve("b"), "1.1.0"));
        store.rollback("1.1.0");

        store.activate("1.2.0", TestSupport.stagedPayload(tmp.resolve("c"), "1.2.0"));

        assertTrue(store.blocked().isEmpty());
    }

    @Test
    void cleanupKeepsOnlyCurrentAndPreviousAndWipesStaging() throws Exception {

        store.activate("1.0.0", TestSupport.stagedPayload(tmp.resolve("a"), "1.0.0"));
        store.activate("1.1.0", TestSupport.stagedPayload(tmp.resolve("b"), "1.1.0"));
        store.activate("1.2.0", TestSupport.stagedPayload(tmp.resolve("c"), "1.2.0"));

        Files.createDirectories(store.stagingDirectory().resolve("leftover"));

        store.cleanup();

        assertFalse(Files.exists(store.versionDirectory("1.0.0")));
        assertTrue(Files.exists(store.versionDirectory("1.1.0")));
        assertTrue(Files.exists(store.versionDirectory("1.2.0")));
        assertFalse(Files.exists(store.stagingDirectory()));
    }

    @Test
    void cleanupNeverFailsNorWaitsWhenThereIsNothingToRemove() throws Exception {

        // Premier démarrage : ni versions/ ni staging/. Le ménage ne doit rien lever ni attendre.
        long start = System.nanoTime();

        store.cleanup();

        assertTrue(System.nanoTime() - start < 2_000_000_000L);
        assertTrue(FileUtil.deleteBestEffort(tmp.resolve("n-existe-pas")));
    }

    @Test
    void bestEffortDeletionRemovesAWholeTree() throws Exception {

        Path tree = Files.createDirectories(tmp.resolve("tree/a/b"));
        Files.writeString(tree.resolve("f.txt"), "x");

        assertTrue(FileUtil.deleteBestEffort(tmp.resolve("tree")));
        assertFalse(Files.exists(tmp.resolve("tree")));
    }

    @Test
    void ignoresPointerFilesThatAreNotValidVersions() throws Exception {

        Files.createDirectories(tmp.resolve("launcher"));
        Files.writeString(tmp.resolve("launcher/current"), "../../etc");

        assertTrue(store.current().isEmpty());
    }
}
