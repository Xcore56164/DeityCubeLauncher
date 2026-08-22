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
     * @return {@code true} si la mise à jour a été appliquée en silence et que l'appelant doit
     * s'attendre à ce que le processus se termine (Windows) ; {@code false} si l'installation a
     * simplement été déléguée au gestionnaire de paquets graphique du système et que l'appelant
     * doit informer l'utilisateur de terminer manuellement (Linux).
     */
    public boolean downloadAndApply(
            LauncherUpdateManifest update,
            ProgressListener listener
    ) throws IOException, InterruptedException {

        String platformKey = resolvePlatformKey();

        if (platformKey == null) {
            throw new IOException(
                    "Aucun gestionnaire de paquets pris en charge (dpkg/rpm) n'a été détecté sur ce système."
            );
        }

        LauncherUpdateTarget target = requireTarget(update, platformKey);

        return switch (OperatingSystem.current()) {
            case WINDOWS -> {
                applyWindowsUpdate(update, target, listener);
                yield true;
            }
            case LINUX -> {
                applyLinuxUpdate(update, target, listener, platformKey);
                yield false;
            }
            default -> throw new IOException("Mise à jour automatique non prise en charge sur cet OS.");
        };
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
    ) throws IOException, InterruptedException {

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

        listener.indeterminate("Mise à jour", "Installation en cours...");

        Process installerProcess = new ProcessBuilder(
                installerPath.toAbsolutePath().toString(),
                "/quiet",
                "/norestart"
        ).start();

        int exitCode = installerProcess.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "L'installeur de mise à jour s'est terminé avec le code " + exitCode
            );
        }

        listener.indeterminate("Mise à jour", "Redémarrage du launcher...");

        relaunchWindowsAndExit();
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

    private void relaunchWindowsAndExit() throws IOException {

        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData == null || localAppData.isBlank()) {
            throw new IOException(
                    "Impossible de déterminer le dossier LOCALAPPDATA pour relancer le launcher."
            );
        }

        Path executable = Path.of(
                localAppData,
                LauncherConfig.LAUNCHER_INSTALL_NAME,
                LauncherConfig.LAUNCHER_INSTALL_NAME + ".exe"
        );

        if (!Files.isRegularFile(executable)) {
            throw new IOException(
                    "Exécutable du launcher introuvable après la mise à jour : " + executable
            );
        }

        new ProcessBuilder(executable.toAbsolutePath().toString()).start();

        System.exit(0);
    }
}
