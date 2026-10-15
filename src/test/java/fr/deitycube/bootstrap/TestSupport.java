package fr.deitycube.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.List;
import java.util.function.BiConsumer;

/** Fabrique de payloads, de clés et d'interface factice pour les tests du bootstrap. */
final class TestSupport {

    private TestSupport() {
    }

    static KeyPair newKeyPair() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    /** Construit le zip d'un payload contenant deux jars factices, signé avec {@code key}. */
    static PayloadBuilder.Result buildPayload(Path workDir, String version, PrivateKey key)
            throws IOException, GeneralSecurityException {

        Path lib = workDir.resolve("lib-" + version);

        Files.createDirectories(lib);
        Files.writeString(lib.resolve("launcher.jar"), "launcher " + version, StandardCharsets.UTF_8);
        Files.writeString(lib.resolve("dependency.jar"), "dependency", StandardCharsets.UTF_8);

        return PayloadBuilder.build(
                lib,
                version,
                "windows",
                "fr.deitycube.launcher.Main",
                List.of("-Xmx64m"),
                workDir.resolve("payload-" + version + ".zip"),
                key
        );
    }

    /** Un payload valide déjà extrait, tel qu'il se présenterait dans staging/. */
    static Path stagedPayload(Path workDir, String version) throws IOException, GeneralSecurityException {

        PayloadBuilder.Result result = buildPayload(workDir, version, newKeyPair().getPrivate());
        Path extracted = workDir.resolve("staged-" + version);

        ZipExtractor.extract(result.zip(), extracted, entries -> {
        });

        return extracted;
    }

    /** Interface scriptable : compte les appels et laisse les tests simuler un antivirus. */
    static final class FakeUi implements BootstrapUi {

        int confirmations;
        int waits;
        int retries;
        boolean confirmAnswer = true;
        boolean retryAnswer = true;
        BiConsumer<Integer, Path> duringWait = (count, folder) -> {
        };

        @Override
        public boolean confirmUpdate(String version, String notes, boolean firstInstall) {
            confirmations++;
            return confirmAnswer;
        }

        @Override
        public Progress progress(String title) {
            return new Progress() {
                @Override
                public void update(String message, long done, long total) {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public void antivirusWait(int seconds, Path folder) {
            duringWait.accept(++waits, folder);
        }

        @Override
        public boolean askRetry(List<String> problems, Path folder) {
            retries++;
            return retryAnswer;
        }

        @Override
        public void warn(String title, String message) {
        }

        @Override
        public void error(String title, String message) {
        }
    }
}
