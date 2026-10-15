package fr.deitycube.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestClientTest {

    private static final String SHA = "ab".repeat(32);

    private static final String MANIFEST = """
            {
              "version": "1.2.0",
              "notes": "Nouveautés",
              "bootstrap_protocol": 1,
              "payload": {
                "windows": { "url": "https://deitycube.fr/downloads/w.zip", "sha256": "%s", "signature": "c2ln" },
                "linux":   { "url": "https://deitycube.fr/downloads/l.zip", "sha256": "%s", "signature": "c2ln" }
              },
              "windows": { "installer_url": "https://deitycube.fr/downloads/old.exe", "sha256": "ignoré" },
              "linux_deb": { "installer_url": "https://deitycube.fr/downloads/old.deb", "sha256": "ignoré" }
            }
            """.formatted(SHA, SHA);

    @Test
    void readsThePayloadOfTheCurrentPlatformAndIgnoresLegacyKeys() throws IOException {

        UpdateManifest windows = client(Os.WINDOWS).parse(MANIFEST);
        UpdateManifest linux = client(Os.LINUX).parse(MANIFEST);

        assertEquals("1.2.0", windows.version());
        assertEquals("Nouveautés", windows.notes());
        assertEquals("https://deitycube.fr/downloads/w.zip", windows.payload().url());
        assertEquals("https://deitycube.fr/downloads/l.zip", linux.payload().url());
    }

    @Test
    void reportsNoPayloadForAPlatformThatHasNone() throws IOException {

        assertNull(client(Os.MACOS).parse(MANIFEST).payload());
    }

    @Test
    void worksWithAnOldStyleManifestThatHasNoPayloadBlock() throws IOException {

        UpdateManifest manifest = client(Os.WINDOWS).parse("""
                { "version": "1.0.4", "windows": { "installer_url": "https://x/y.exe", "sha256": "z" } }
                """);

        assertEquals("1.0.4", manifest.version());
        assertNull(manifest.payload());
        assertEquals(1, manifest.protocol());
    }

    @Test
    void rejectsAManifestWhoseVersionCouldBeUsedAsAPath() {

        assertThrows(IOException.class, () -> client(Os.WINDOWS).parse("""
                { "version": "../../evil", "payload": {} }
                """));
    }

    @Test
    void rejectsAMalformedPayloadBlockForThisPlatform() {

        assertThrows(IOException.class, () -> client(Os.WINDOWS).parse("""
                { "version": "1.2.0", "payload": { "windows": { "url": "https://x/w.zip", "sha256": "trop-court", "signature": "c2ln" } } }
                """));

        assertThrows(IOException.class, () -> client(Os.WINDOWS).parse("""
                { "version": "1.2.0", "payload": { "windows": { "url": "https://x/w.zip", "sha256": "%s" } } }
                """.formatted(SHA)));
    }

    @Test
    void strictPolicyOnlyAllowsHttps() throws IOException {

        UrlPolicy strict = UrlPolicy.strict();

        assertNotNull(strict.check("https://deitycube.fr/x"));
        assertThrows(IOException.class, () -> strict.check("http://deitycube.fr/x"));
        assertThrows(IOException.class, () -> strict.check("http://localhost:8000/x"));
        assertThrows(IOException.class, () -> strict.check("file:///etc/passwd"));
        assertThrows(IOException.class, () -> strict.check("ftp://deitycube.fr/x"));
    }

    @Test
    void loopbackPolicyOnlyOpensPlainHttpToTheLocalMachine() throws IOException {

        UrlPolicy loopback = UrlPolicy.allowLoopbackHttp();

        assertNotNull(loopback.check("http://localhost:8000/x"));
        assertNotNull(loopback.check("http://127.0.0.1:8000/x"));
        assertTrue(UrlPolicy.isLoopback("[::1]"));

        assertThrows(IOException.class, () -> loopback.check("http://deitycube.fr/x"));
        assertThrows(IOException.class, () -> loopback.check("http://localhost.evil.com/x"));
        assertThrows(IOException.class, () -> loopback.check("http://192.168.1.10/x"));
    }

    private ManifestClient client(Os os) {
        return new ManifestClient(new HttpFetcher(), UrlPolicy.strict(), os);
    }
}
