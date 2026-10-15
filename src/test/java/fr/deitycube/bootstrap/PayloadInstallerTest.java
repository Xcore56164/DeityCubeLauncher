package fr.deitycube.bootstrap;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadInstallerTest {

    @TempDir
    Path tmp;

    private HttpServer server;
    private KeyPair keys;
    private VersionStore store;
    private TestSupport.FakeUi ui;
    private PayloadBuilder.Result payload;

    @BeforeEach
    void setUp() throws Exception {

        keys = TestSupport.newKeyPair();
        store = new VersionStore(tmp.resolve("launcher"));
        ui = new TestSupport.FakeUi();
        payload = TestSupport.buildPayload(tmp.resolve("build"), "1.2.0", keys.getPrivate());

        serve("/payload.zip", payload);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void installsAValidPayloadAndMakesItCurrent() throws Exception {

        installer(keys).install(manifest("1.2.0", payload.sha256(), payload.signature()));

        assertEquals(Optional.of("1.2.0"), store.current());
        assertTrue(store.intactCurrent().isPresent());
        assertEquals(1, ui.waits, "l'attente antivirus doit avoir lieu");
        assertFalse(Files.exists(store.stagingDirectory().resolve("payload-1.2.0.zip")));
    }

    @Test
    void refusesADownloadWhoseHashDoesNotMatchTheManifest() {

        IOException error = assertThrows(IOException.class, () ->
                installer(keys).install(manifest("1.2.0", "00".repeat(32), payload.signature())));

        assertTrue(error.getMessage().contains("SHA-256"));
        assertTrue(store.current().isEmpty());
    }

    @Test
    void refusesAPayloadSignedByAnotherKey() throws Exception {

        // Le serveur de téléchargement est compromis : il sert son propre payload, avec ses
        // propres hashes et sa propre signature. Sans la clé privée de DeityCube, ça ne passe pas.
        KeyPair attacker = TestSupport.newKeyPair();
        PayloadBuilder.Result malicious = TestSupport.buildPayload(tmp.resolve("evil"), "9.9.9", attacker.getPrivate());

        serve("/payload.zip", malicious);

        IOException error = assertThrows(IOException.class, () ->
                installer(keys).install(manifest("9.9.9", malicious.sha256(), malicious.signature())));

        assertTrue(error.getMessage().contains("Signature invalide"));
        assertTrue(store.current().isEmpty());
        assertEquals(0, ui.waits, "rien ne doit être extrait avant la vérification de la signature");
    }

    @Test
    void refusesAPayloadRelabelledAsANewerVersion() throws Exception {

        // Rejeu : un vieux payload signé (1.2.0) présenté avec la signature d'une autre version.
        IOException error = assertThrows(IOException.class, () ->
                installer(keys).install(manifest("1.3.0", payload.sha256(), payload.signature())));

        assertTrue(error.getMessage().contains("Signature invalide"));
        assertTrue(store.current().isEmpty());
    }

    @Test
    void refusesAPayloadWhoseContentAnnouncesAnotherVersion() throws Exception {

        // Signé correctement pour "1.3.0", mais payload.json dit 1.2.0.
        String signature = PayloadSignature.sign(
                keys.getPrivate(),
                PayloadSignature.message("1.3.0", "windows", payload.sha256())
        );

        IOException error = assertThrows(IOException.class, () ->
                installer(keys).install(manifest("1.3.0", payload.sha256(), signature)));

        assertTrue(error.getMessage().contains("annonce la version"));
        assertTrue(store.current().isEmpty());
    }

    @Test
    void retriesWhenAnAntivirusDeletesAFileDuringTheWait() throws Exception {

        // Premier passage : l'antivirus supprime un jar pendant l'attente. L'utilisateur ajoute
        // l'exclusion et clique sur Réessayer : le second passage doit aboutir.
        ui.duringWait = (count, folder) -> {
            if (count == 1) {
                deleteQuietly(store.stagingDirectory().resolve("extracted-1.2.0/lib/launcher.jar"));
            }
        };

        installer(keys).install(manifest("1.2.0", payload.sha256(), payload.signature()));

        assertEquals(1, ui.retries);
        assertEquals(2, ui.waits);
        assertTrue(store.intactCurrent().isPresent());
    }

    @Test
    void retriesWhenTheArchiveItselfIsQuarantined() throws Exception {

        ui.duringWait = (count, folder) -> {
            if (count == 1) {
                deleteQuietly(store.stagingDirectory().resolve("payload-1.2.0.zip"));
                deleteQuietly(store.stagingDirectory().resolve("extracted-1.2.0/lib/dependency.jar"));
            }
        };

        installer(keys).install(manifest("1.2.0", payload.sha256(), payload.signature()));

        assertTrue(store.intactCurrent().isPresent());
    }

    @Test
    void cancellingAfterABlockedFileLeavesTheInstalledVersionUntouched() throws Exception {

        PayloadBuilder.Result v1 = TestSupport.buildPayload(tmp.resolve("v1"), "1.0.0", keys.getPrivate());
        serve("/payload.zip", v1);
        installer(keys).install(manifest("1.0.0", v1.sha256(), v1.signature()));

        serve("/payload.zip", payload);
        ui.retryAnswer = false;
        ui.duringWait = (count, folder) ->
                deleteQuietly(store.stagingDirectory().resolve("extracted-1.2.0/lib/launcher.jar"));

        assertThrows(PayloadInstaller.CancelledException.class, () ->
                installer(keys).install(manifest("1.2.0", payload.sha256(), payload.signature())));

        assertEquals(Optional.of("1.0.0"), store.current());
        assertTrue(store.intactCurrent().isPresent());
        assertFalse(Files.exists(store.versionDirectory("1.2.0")));
    }

    @Test
    void aFailedUpdateNeverTouchesTheInstalledVersion() throws Exception {

        PayloadBuilder.Result v1 = TestSupport.buildPayload(tmp.resolve("v1"), "1.0.0", keys.getPrivate());
        serve("/payload.zip", v1);
        installer(keys).install(manifest("1.0.0", v1.sha256(), v1.signature()));

        serve("/payload.zip", payload);

        assertThrows(IOException.class, () ->
                installer(keys).install(manifest("1.2.0", "00".repeat(32), payload.signature())));

        assertEquals(Optional.of("1.0.0"), store.current());
        assertTrue(store.intactCurrent().isPresent());
    }

    @Test
    void refusesNonHttpsUrlsOutsideTheTestLoopback() {

        PayloadInstaller strict = new PayloadInstaller(
                new HttpFetcher(), UrlPolicy.strict(), keys.getPublic(), ui, store, 0, "windows"
        );

        assertThrows(IOException.class, () ->
                strict.install(manifest("1.2.0", payload.sha256(), payload.signature())));
    }

    private PayloadInstaller installer(KeyPair signingKeys) {

        return new PayloadInstaller(
                new HttpFetcher(),
                UrlPolicy.allowLoopbackHttp(),
                signingKeys.getPublic(),
                ui,
                store,
                0,
                "windows"
        );
    }

    private UpdateManifest manifest(String version, String sha256, String signature) {

        return new UpdateManifest(
                version,
                "",
                1,
                new UpdateManifest.PayloadTarget(
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/payload.zip",
                        sha256,
                        signature
                )
        );
    }

    /** (Re)démarre un serveur local qui sert {@code content} à l'adresse {@code path}. */
    private void serve(String path, PayloadBuilder.Result content) throws IOException {

        if (server != null) {
            server.stop(0);
        }

        byte[] bytes = Files.readAllBytes(content.zip());

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    private static void deleteQuietly(Path file) {

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
