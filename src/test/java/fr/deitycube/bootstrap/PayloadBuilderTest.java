package fr.deitycube.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadBuilderTest {

    @TempDir
    Path tmp;

    @Test
    void theZipIsSelfDescribingAndItsSignatureVerifies() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        PayloadBuilder.Result result = TestSupport.buildPayload(tmp, "1.2.0", keys.getPrivate());

        Path extracted = tmp.resolve("extracted");
        ZipExtractor.extract(result.zip(), extracted, entries -> {
        });

        PayloadDescriptor descriptor = PayloadDescriptor.read(extracted);

        assertEquals("1.2.0", descriptor.version());
        assertEquals("fr.deitycube.launcher.Main", descriptor.mainClass());
        assertEquals(List.of("-Xmx64m"), descriptor.jvmArgs());
        assertEquals(2, descriptor.files().size());
        assertTrue(descriptor.corruptedFiles(extracted).isEmpty());

        assertTrue(PayloadSignature.verify(
                keys.getPublic(),
                PayloadSignature.message("1.2.0", "windows", FileUtil.sha256(result.zip())),
                result.signature()
        ));
    }

    @Test
    void identicalInputsGiveByteIdenticalZips() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        PayloadBuilder.Result first = TestSupport.buildPayload(tmp.resolve("one"), "1.2.0", keys.getPrivate());
        PayloadBuilder.Result second = TestSupport.buildPayload(tmp.resolve("two"), "1.2.0", keys.getPrivate());

        assertArrayEquals(Files.readAllBytes(first.zip()), Files.readAllBytes(second.zip()));
        assertEquals(first.sha256(), second.sha256());
    }

    @Test
    void writesTheSignatureAndHashNextToTheZip() throws Exception {

        PayloadBuilder.Result result = TestSupport.buildPayload(tmp, "1.2.0", TestSupport.newKeyPair().getPrivate());

        assertEquals(result.signature(), Files.readString(Path.of(result.zip() + ".sig")).trim());
        assertEquals(result.sha256(), Files.readString(Path.of(result.zip() + ".sha256")).trim());
    }

    @Test
    void withoutAKeyThePayloadIsLeftUnsigned() throws Exception {

        PayloadBuilder.Result result = TestSupport.buildPayload(tmp, "1.2.0", null);

        assertEquals(null, result.signature());
        assertFalse(Files.exists(Path.of(result.zip() + ".sig")));
    }

    @Test
    void refusesVersionsThatAreNotPlainNumbers() {

        assertThrows(IllegalArgumentException.class, () ->
                TestSupport.buildPayload(tmp, "1.0.6-dev", null));
    }

    @Test
    void detectsMissingResizedAndAlteredFiles() throws Exception {

        Path staged = TestSupport.stagedPayload(tmp, "1.2.0");
        PayloadDescriptor descriptor = PayloadDescriptor.read(staged);

        assertTrue(descriptor.corruptedFiles(staged).isEmpty());

        Files.delete(staged.resolve("lib/launcher.jar"));
        assertEquals(1, descriptor.missingFiles(staged).size());
        assertTrue(descriptor.missingFiles(staged).get(0).contains("absent"));

        Files.writeString(staged.resolve("lib/launcher.jar"), "x");
        assertTrue(descriptor.missingFiles(staged).get(0).contains("taille"));

        // Même taille, contenu différent : seul le contrôle complet le voit.
        Files.writeString(staged.resolve("lib/launcher.jar"), "launcher 9.9.9");
        assertTrue(descriptor.missingFiles(staged).isEmpty());
        assertEquals(1, descriptor.corruptedFiles(staged).size());
        assertTrue(descriptor.corruptedFiles(staged).get(0).contains("modifié"));
    }

    @Test
    void rejectsDescriptorsThatListUnsafePaths() {

        String json = """
                { "version": "1.2.0", "main_class": "a.B", "files": [
                  { "path": "../../evil.dll", "size": 1, "sha256": "%s" } ] }
                """.formatted("ab".repeat(32));

        assertThrows(IOException.class, () -> PayloadDescriptor.parse(json.getBytes()));
    }
}
