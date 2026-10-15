package fr.deitycube.bootstrap;

import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 * Configuration d'exécution. En production tout est figé dans le code. Les variables
 * d'environnement DEITYCUBE_* ne sont prises en compte que si DEITYCUBE_MANIFEST_URL désigne
 * une adresse en boucle locale (http://localhost…) : c'est le mode des tests de bout en bout,
 * impossible à utiliser pour faire pointer un vrai bootstrap vers un serveur distant.
 */
final class BootstrapEnvironment {

    static final String MANIFEST_URL = "https://deitycube.fr/downloads/launcher_manifest.json";

    private static final String APP_FOLDER = "DeityCube";
    private static final int DEFAULT_ANTIVIRUS_WAIT_SECONDS = 10;

    private final String manifestUrl;
    private final Path root;
    private final String publicKeyOverride;
    private final boolean autoAccept;
    private final int antivirusWaitSeconds;
    private final UrlPolicy urlPolicy;

    private BootstrapEnvironment(
            String manifestUrl,
            Path root,
            String publicKeyOverride,
            boolean autoAccept,
            int antivirusWaitSeconds,
            UrlPolicy urlPolicy
    ) {
        this.manifestUrl = manifestUrl;
        this.root = root;
        this.publicKeyOverride = publicKeyOverride;
        this.autoAccept = autoAccept;
        this.antivirusWaitSeconds = antivirusWaitSeconds;
        this.urlPolicy = urlPolicy;
    }

    static BootstrapEnvironment detect() {

        String override = System.getenv("DEITYCUBE_MANIFEST_URL");

        if (override != null && isLoopbackUrl(override)) {

            String root = System.getenv("DEITYCUBE_BOOTSTRAP_ROOT");

            return new BootstrapEnvironment(
                    override,
                    root != null && !root.isBlank() ? Path.of(root) : defaultRoot(),
                    System.getenv("DEITYCUBE_PAYLOAD_PUBKEY"),
                    "1".equals(System.getenv("DEITYCUBE_BOOTSTRAP_AUTO")),
                    parseSeconds(System.getenv("DEITYCUBE_AV_WAIT_SECONDS")),
                    UrlPolicy.allowLoopbackHttp()
            );
        }

        return new BootstrapEnvironment(
                MANIFEST_URL,
                defaultRoot(),
                null,
                false,
                DEFAULT_ANTIVIRUS_WAIT_SECONDS,
                UrlPolicy.strict()
        );
    }

    String manifestUrl() {
        return manifestUrl;
    }

    Path root() {
        return root;
    }

    boolean autoAccept() {
        return autoAccept;
    }

    int antivirusWaitSeconds() {
        return antivirusWaitSeconds;
    }

    UrlPolicy urlPolicy() {
        return urlPolicy;
    }

    PublicKey publicKey() throws GeneralSecurityException {

        return publicKeyOverride != null && !publicKeyOverride.isBlank()
                ? PayloadSignature.parsePublicKey(publicKeyOverride)
                : PayloadSignature.embeddedKey();
    }

    /**
     * CONTRAT FIGÉ : même dossier que GameDirectory.getRootDirectory() dans le launcher. Dupliqué
     * ici volontairement, le bootstrap ne dépend d'aucune classe du launcher.
     */
    static Path defaultRoot() {

        return switch (Os.current()) {

            case WINDOWS -> {
                String appData = System.getenv("APPDATA");

                if (appData == null || appData.isBlank()) {
                    throw new IllegalStateException("Dossier AppData introuvable.");
                }

                yield Path.of(appData, APP_FOLDER);
            }

            case MACOS -> Path.of(System.getProperty("user.home"), "Library", "Application Support", APP_FOLDER);

            default -> {
                String xdgDataHome = System.getenv("XDG_DATA_HOME");

                yield xdgDataHome != null && !xdgDataHome.isBlank()
                        ? Path.of(xdgDataHome, APP_FOLDER)
                        : Path.of(System.getProperty("user.home"), ".local", "share", APP_FOLDER);
            }
        };
    }

    private static boolean isLoopbackUrl(String url) {

        try {
            URI uri = URI.create(url);

            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && UrlPolicy.isLoopback(uri.getHost());

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static int parseSeconds(String value) {

        try {
            return value == null ? DEFAULT_ANTIVIRUS_WAIT_SECONDS : Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_ANTIVIRUS_WAIT_SECONDS;
        }
    }
}
