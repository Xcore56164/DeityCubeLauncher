package fr.deitycube.launcher.update;

import fr.deitycube.launcher.config.AppVersion;
import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.platform.OperatingSystem;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.progress.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherUpdater {

    private static final String PLATFORM_WINDOWS = "windows";
    private static final String PLATFORM_LINUX_DEB = "linux_deb";
    private static final String PLATFORM_LINUX_RPM = "linux_rpm";

    public LauncherUpdateManifest checkForUpdate() throws IOException {

        LauncherUpdateManifest manifest =
                LauncherUpdateManifestReader.read(LauncherConfig.LAUNCHER_UPDATE_MANIFEST_URL);

        if (!AppVersion.isNewer(manifest.getVersion())) {
            return null;
        }

        String platformKey = resolvePlatformKey();

        return platformKey != null && manifest.getTarget(platformKey) != null ? manifest : null;
    }

    /**
     * Télécharge la mise à jour et démarre son installation, sans attendre qu'elle se termine.
     * <p>
     * L'installeur Windows (WiX) fait une mise à niveau majeure MSI : il désinstalle l'ancienne
     * version puis installe la nouvelle. Si on reste bloqué à attendre ce process (par ex. avec
     * {@code Process.waitFor()}) et qu'un antivirus le tue entre-temps — ce qui arrive plus
     * souvent qu'on ne le voudrait pour un exécutable fraîchement téléchargé et non signé — le
     * launcher se retrouve désinstallé sans que la nouvelle version ait pu s'installer. Se
     * désengager immédiatement après avoir lancé l'installeur (au lieu d'attendre dessus puis de
     * relancer automatiquement) évite ce scénario : l'installation continue de façon
     * indépendante, et c'est à l'utilisateur de relancer le launcher une fois terminée.
     */
    public void downloadAndApply(
            LauncherUpdateManifest update,
            ProgressListener listener
    ) throws IOException {

        String platformKey = resolvePlatformKey();

        if (platformKey == null) {
            throw new IOException(
                    "Aucun gestionnaire de paquets pris en charge (dpkg/rpm) n'a été détecté sur ce système."
            );
        }

        LauncherUpdateTarget target = requireTarget(update, platformKey);

        switch (OperatingSystem.current()) {
            case WINDOWS -> applyWindowsUpdate(update, target, listener);
            case LINUX -> applyLinuxUpdate(update, target, listener, platformKey);
            default -> throw new IOException("Mise à jour automatique non prise en charge sur cet OS.");
        }
    }

    private LauncherUpdateTarget requireTarget(
            LauncherUpdateManifest update,
            String platformKey
    ) throws IOException {

        LauncherUpdateTarget target = update.getTarget(platformKey);

        if (target == null
                || target.getInstallerUrl() == null || target.getInstallerUrl().isBlank()
                || target.getSha256() == null || target.getSha256().isBlank()) {

            throw new IOException(
                    "Le manifeste de mise à jour ne fournit pas de cible valide pour la plateforme '"
                            + platformKey + "'."
            );
        }

        if (!target.getInstallerUrl().startsWith("https://")) {
            throw new IOException(
                    "L'URL de l'installeur doit être en HTTPS pour la plateforme '" + platformKey + "'."
            );
        }

        return target;
    }

    private void applyWindowsUpdate(
            LauncherUpdateManifest update,
            LauncherUpdateTarget target,
            ProgressListener listener
    ) throws IOException {

        listener.indeterminate("Mise à jour", "Téléchargement de la version " + update.getVersion() + "...");

        Path installerPath = GameDirectory.getCacheDirectory()
                .resolve("updates")
                .resolve(LauncherConfig.LAUNCHER_INSTALL_NAME + "-" + update.getVersion() + ".exe");

        HttpDownloader.downloadSha256(
                target.getInstallerUrl(),
                installerPath,
                target.getSha256(),
                bytes -> listener.indeterminate(
                        "Mise à jour",
                        "Téléchargement... " + (bytes / (1024 * 1024)) + " Mo"
                )
        );

        listener.indeterminate("Mise à jour", "Lancement de l'installeur...");

        // /passive (barre de progression visible, sans interaction requise) plutôt que /quiet :
        // un process totalement invisible qui s'exécute juste après le téléchargement d'un
        // fichier ressemble beaucoup plus à un comportement de malware aux yeux d'un antivirus
        // qu'une installation avec une fenêtre visible, ce qui réduit le risque qu'il soit tué
        // en cours de route.
        new ProcessBuilder(
                installerPath.toAbsolutePath().toString(),
                "/passive",
                "/norestart"
        ).start();
    }

    private void applyLinuxUpdate(
            LauncherUpdateManifest update,
            LauncherUpdateTarget target,
            ProgressListener listener,
            String platformKey
    ) throws IOException {

        listener.indeterminate("Mise à jour", "Téléchargement de la version " + update.getVersion() + "...");

        String extension = PLATFORM_LINUX_RPM.equals(platformKey) ? ".rpm" : ".deb";

        Path packagePath = GameDirectory.getCacheDirectory()
                .resolve("updates")
                .resolve(LauncherConfig.LAUNCHER_INSTALL_NAME.toLowerCase() + "-" + update.getVersion() + extension);

        HttpDownloader.downloadSha256(
                target.getInstallerUrl(),
                packagePath,
                target.getSha256(),
                bytes -> listener.indeterminate(
                        "Mise à jour",
                        "Téléchargement... " + (bytes / (1024 * 1024)) + " Mo"
                )
        );

        listener.indeterminate("Mise à jour", "Ouverture du gestionnaire de paquets...");

        // dpkg/rpm exigent les droits root : impossible d'installer en silence comme sur Windows.
        // xdg-open délègue au gestionnaire graphique du système (GNOME Software, Discover...),
        // qui demandera lui-même l'élévation à l'utilisateur.
        new ProcessBuilder("xdg-open", packagePath.toAbsolutePath().toString()).start();
    }

    private String resolvePlatformKey() {

        return switch (OperatingSystem.current()) {

            case WINDOWS -> PLATFORM_WINDOWS;

            case LINUX -> {
                if (Files.exists(Path.of("/etc/debian_version"))) {
                    yield PLATFORM_LINUX_DEB;
                }
                if (isOnPath("rpm")) {
                    yield PLATFORM_LINUX_RPM;
                }
                yield null;
            }

            default -> null;
        };
    }

    private boolean isOnPath(String executable) {

        String path = System.getenv("PATH");

        if (path == null || path.isBlank()) {
            return false;
        }

        for (String directory : path.split(File.pathSeparator)) {
            if (Files.isExecutable(Path.of(directory, executable))) {
                return true;
            }
        }

        return false;
    }
}
