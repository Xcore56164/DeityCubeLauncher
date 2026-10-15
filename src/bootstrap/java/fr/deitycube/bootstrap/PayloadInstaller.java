package fr.deitycube.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;

/**
 * Installe un payload sans jamais toucher à la version active : téléchargement, vérification du
 * SHA-256 puis de la signature, extraction dans staging/, attente pour l'antivirus, contrôle de
 * chaque fichier extrait, et seulement alors bascule atomique (voir {@link VersionStore}).
 */
final class PayloadInstaller {

    static final class CancelledException extends Exception {
        CancelledException() {
            super("Installation annulée par l'utilisateur");
        }
    }

    private final HttpFetcher http;
    private final UrlPolicy policy;
    private final PublicKey publicKey;
    private final BootstrapUi ui;
    private final VersionStore store;
    private final int antivirusWaitSeconds;
    private final String platform;

    PayloadInstaller(
            HttpFetcher http,
            UrlPolicy policy,
            PublicKey publicKey,
            BootstrapUi ui,
            VersionStore store,
            int antivirusWaitSeconds,
            String platform
    ) {
        this.http = http;
        this.policy = policy;
        this.publicKey = publicKey;
        this.ui = ui;
        this.store = store;
        this.antivirusWaitSeconds = antivirusWaitSeconds;
        this.platform = platform;
    }

    void install(UpdateManifest manifest) throws IOException, CancelledException, InterruptedException {

        UpdateManifest.PayloadTarget target = manifest.payload();
        String version = manifest.version();

        URI uri = policy.check(target.url());

        Path staging = store.stagingDirectory();
        Path zip = staging.resolve("payload-" + version + ".zip");
        Path extracted = staging.resolve("extracted-" + version);

        Files.createDirectories(staging);
        FileUtil.deleteRecursively(extracted);

        while (true) {

            String sha256 = downloadIfNeeded(uri, target, version, zip);

            verifySignature(version, sha256, target, zip);

            extract(zip, extracted);

            ui.antivirusWait(antivirusWaitSeconds, store.launcherDirectory());

            List<String> problems = check(extracted, version);

            if (problems.isEmpty()) {
                break;
            }

            BootstrapLog.info("Fichiers manquants ou modifiés après extraction : " + problems);

            if (!ui.askRetry(problems, store.launcherDirectory())) {
                FileUtil.deleteRecursively(staging);
                throw new CancelledException();
            }

            FileUtil.deleteRecursively(extracted);
        }

        store.activate(version, extracted);

        Files.deleteIfExists(zip);
    }

    private String downloadIfNeeded(
            URI uri,
            UpdateManifest.PayloadTarget target,
            String version,
            Path zip
    ) throws IOException {

        if (Files.isRegularFile(zip) && FileUtil.sha256(zip).equalsIgnoreCase(target.sha256())) {
            return target.sha256();
        }

        String title = "Téléchargement du launcher " + version + "…";

        String actual;

        try (BootstrapUi.Progress progress = ui.progress(title)) {

            actual = http.download(uri, zip, (done, total) -> progress.update(
                    title + "  " + megabytes(done) + (total > 0 ? " / " + megabytes(total) : ""),
                    done,
                    total
            ));
        }

        if (!actual.equalsIgnoreCase(target.sha256())) {
            Files.deleteIfExists(zip);
            throw new IOException("Empreinte SHA-256 inattendue : le téléchargement est corrompu ou a été altéré.");
        }

        return actual;
    }

    private void verifySignature(
            String version,
            String sha256,
            UpdateManifest.PayloadTarget target,
            Path zip
    ) throws IOException {

        byte[] message = PayloadSignature.message(version, platform, sha256);

        if (!PayloadSignature.verify(publicKey, message, target.signature())) {
            Files.deleteIfExists(zip);
            throw new IOException("Signature invalide : ce payload n'a pas été publié par DeityCube, il est refusé.");
        }
    }

    private void extract(Path zip, Path extracted) throws IOException {

        FileUtil.deleteRecursively(extracted);

        try (BootstrapUi.Progress progress = ui.progress("Extraction du launcher…")) {

            ZipExtractor.extract(zip, extracted, entries ->
                    progress.update("Extraction du launcher…  " + entries + " fichiers", entries, -1)
            );
        }
    }

    /**
     * @return les fichiers à problème (vide si tout est en ordre)
     * @throws IOException si le payload est incohérent avec le manifeste (non réessayable)
     */
    private List<String> check(Path extracted, String version) throws IOException {

        PayloadDescriptor descriptor;

        try {
            descriptor = PayloadDescriptor.read(extracted);
        } catch (IOException e) {
            // Typiquement : payload.json supprimé par l'antivirus. L'utilisateur peut réessayer.
            return List.of(PayloadDescriptor.FILE_NAME + " (" + e.getMessage() + ")");
        }

        if (!descriptor.version().equals(version)) {
            throw new IOException(
                    "Le payload annonce la version " + descriptor.version() + " au lieu de " + version + "."
            );
        }

        if (descriptor.protocol() > BootstrapMain.PROTOCOL) {
            throw new IOException(
                    "Ce launcher exige une version plus récente du programme d'installation (protocole "
                            + descriptor.protocol() + ")."
            );
        }

        return descriptor.corruptedFiles(extracted);
    }

    private static String megabytes(long bytes) {
        return String.format("%.1f Mo", bytes / 1_048_576.0);
    }
}
