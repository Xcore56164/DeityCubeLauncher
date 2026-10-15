package fr.deitycube.bootstrap;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Micro-logiciel installé une seule fois avec le runtime : il met à jour puis lance le launcher.
 * <p>
 * IL NE DOIT PLUS CHANGER d'une version du launcher à l'autre : tout ce qui peut évoluer vit dans
 * les payloads téléchargés. Les points de contrat (chemins, clés du manifeste, schéma de
 * payload.json, message signé, clé publique) sont signalés « CONTRAT FIGÉ » dans leurs classes.
 */
public final class BootstrapMain {

    /** Version du contrat bootstrap/payload. Un payload qui en exige une plus récente est refusé. */
    static final int PROTOCOL = 1;

    private static final int WATCH_SECONDS = 20;
    private static final String DOWNLOAD_PAGE = "https://deitycube.fr";

    private final BootstrapEnvironment env;
    private final BootstrapUi ui;

    private BootstrapMain(BootstrapEnvironment env, BootstrapUi ui) {
        this.env = env;
        this.ui = ui;
    }

    public static void main(String[] args) {

        int code;

        try {
            BootstrapEnvironment env = BootstrapEnvironment.detect();

            BootstrapUi ui = env.autoAccept()
                    ? new NonInteractiveUi(true)
                    : GraphicsEnvironment.isHeadless()
                            ? new NonInteractiveUi(false)
                            : new SwingUi();

            code = new BootstrapMain(env, ui).run();

        } catch (Throwable error) {
            BootstrapLog.error("Erreur fatale du bootstrap", error);
            code = 1;
        }

        System.exit(code);
    }

    private int run() {

        BootstrapLog.init(env.root().resolve("logs"));
        BootstrapLog.info("Démarrage du bootstrap (protocole " + PROTOCOL + ", Java " + System.getProperty("java.version") + ")");

        Path launcherDirectory = env.root().resolve("launcher");

        try (InstanceLock lock = InstanceLock.tryAcquire(launcherDirectory.resolve("bootstrap.lock"))) {

            if (lock == null) {
                BootstrapLog.info("Un autre bootstrap est déjà en cours d'exécution : abandon");
                return 0;
            }

            return runLocked(new VersionStore(launcherDirectory));

        } catch (Exception error) {
            BootstrapLog.error("Erreur du bootstrap", error);
            ui.error("DeityCube Launcher", "Le launcher n'a pas pu démarrer :\n" + error.getMessage()
                    + "\n\nJournal : " + env.root().resolve("logs").resolve("bootstrap.log"));
            return 1;
        }
    }

    private int runLocked(VersionStore store) throws IOException {

        Optional<PayloadDescriptor> installed = store.intactCurrent();

        boolean firstInstall = installed.isEmpty();

        installed.ifPresent(descriptor -> BootstrapLog.info("Version installée : " + descriptor.version()));

        if (firstInstall) {
            BootstrapLog.info("Aucune version utilisable installée (première installation ou réparation)");
        }

        UpdateManifest manifest = fetchManifest();

        if (manifest != null && manifest.payload() != null) {

            boolean needed = firstInstall || Versions.isNewer(manifest.version(), installed.get().version());
            boolean blocked = store.blocked().filter(manifest.version()::equals).isPresent();

            if (needed && !blocked) {

                boolean done = update(manifest, store, firstInstall);

                if (!done && firstInstall) {
                    return 1;
                }
            }
        }

        installed = store.intactCurrent();

        if (installed.isEmpty()) {

            ui.error(
                    "DeityCube Launcher",
                    "Aucune version du launcher n'est installée, et le serveur de mise à jour est injoignable.\n"
                            + "Vérifiez votre connexion internet puis relancez."
            );

            return 1;
        }

        try {
            store.cleanup();
        } catch (IOException e) {
            BootstrapLog.error("Nettoyage des anciennes versions impossible", e);
        }

        return launch(store, installed.get());
    }

    private UpdateManifest fetchManifest() {

        try {
            UpdateManifest manifest = new ManifestClient(new HttpFetcher(), env.urlPolicy(), Os.current())
                    .fetch(env.manifestUrl());

            BootstrapLog.info("Manifeste : version " + manifest.version()
                    + (manifest.payload() == null ? " (aucun payload pour cet OS)" : ""));

            return manifest;

        } catch (IOException e) {
            BootstrapLog.error("Manifeste inaccessible ou invalide (on continue avec la version installée)", e);
            return null;
        }
    }

    /** @return {@code true} si la nouvelle version est installée et active */
    private boolean update(UpdateManifest manifest, VersionStore store, boolean firstInstall) {

        if (manifest.protocol() > PROTOCOL) {

            ui.error(
                    "Mise à jour du programme d'installation requise",
                    "La version " + manifest.version() + " du launcher nécessite un programme d'installation plus "
                            + "récent.\nTéléchargez-le sur " + DOWNLOAD_PAGE + " puis réinstallez le launcher."
            );

            openWebsite();

            return false;
        }

        if (!ui.confirmUpdate(manifest.version(), manifest.notes(), firstInstall)) {
            return false;
        }

        PublicKey key;

        try {
            key = env.publicKey();
        } catch (GeneralSecurityException e) {
            BootstrapLog.error("Clé publique de signature inutilisable", e);
            ui.error("DeityCube Launcher", "Ce programme d'installation est mal configuré :\n" + e.getMessage());
            return false;
        }

        PayloadInstaller installer = new PayloadInstaller(
                new HttpFetcher(),
                env.urlPolicy(),
                key,
                ui,
                store,
                env.antivirusWaitSeconds(),
                Os.current().payloadKey()
        );

        try {
            installer.install(manifest);
            BootstrapLog.info("Version " + manifest.version() + " installée");

            return true;

        } catch (PayloadInstaller.CancelledException e) {
            BootstrapLog.info("Installation annulée par l'utilisateur");
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;

        } catch (IOException e) {
            BootstrapLog.error("Installation de la version " + manifest.version() + " impossible", e);

            ui.error(
                    "Mise à jour impossible",
                    e.getMessage() + (firstInstall ? "" : "\n\nLa version actuelle va être lancée.")
            );

            return false;
        }
    }

    private int launch(VersionStore store, PayloadDescriptor descriptor) {

        Path logFile = env.root().resolve("logs").resolve("launcher-process.log");

        try {
            int code = startAndWatch(store, descriptor, logFile);

            if (code == 0) {
                return 0;
            }

            BootstrapLog.info("Le launcher " + descriptor.version() + " s'est arrêté dès le démarrage (code " + code + ")");

            Optional<String> fallback = store.rollback(descriptor.version());

            if (fallback.isEmpty()) {

                ui.error(
                        "DeityCube Launcher",
                        "Le launcher n'a pas pu démarrer (code " + code + ").\n\nJournal : " + logFile
                );

                return 1;
            }

            ui.warn(
                    "Retour à la version précédente",
                    "La version " + descriptor.version() + " du launcher n'a pas pu démarrer.\n"
                            + "La version " + fallback.get() + " est relancée à la place."
            );

            Optional<PayloadDescriptor> previous = store.intact(fallback.get());

            return previous.isPresent() ? startAndWatch(store, previous.get(), logFile) : 1;

        } catch (IOException e) {
            BootstrapLog.error("Lancement impossible", e);
            ui.error("DeityCube Launcher", "Impossible de lancer le launcher :\n" + e.getMessage());
            return 1;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 1;
        }
    }

    /**
     * @return 0 si le launcher tourne (ou s'est fermé normalement), sinon son code de sortie s'il
     * s'est arrêté dans les premières secondes : signe qu'il n'a pas pu démarrer
     */
    private int startAndWatch(VersionStore store, PayloadDescriptor descriptor, Path logFile)
            throws IOException, InterruptedException {

        Process process = LauncherProcess.start(store.versionDirectory(descriptor.version()), descriptor, logFile);

        if (!process.waitFor(WATCH_SECONDS, TimeUnit.SECONDS)) {
            BootstrapLog.info("Launcher " + descriptor.version() + " démarré, le bootstrap se termine");
            return 0;
        }

        return process.exitValue();
    }

    private void openWebsite() {

        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(DOWNLOAD_PAGE));
            }
        } catch (IOException | UnsupportedOperationException e) {
            BootstrapLog.error("Impossible d'ouvrir " + DOWNLOAD_PAGE, e);
        }
    }
}
