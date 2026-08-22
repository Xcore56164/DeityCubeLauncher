package fr.deitycube.launcher.update;

import fr.deitycube.launcher.config.AppVersion;
import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.progress.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherUpdater {

    public LauncherUpdateManifest checkForUpdate() throws IOException {

        LauncherUpdateManifest manifest =
                LauncherUpdateManifestReader.read(LauncherConfig.LAUNCHER_UPDATE_MANIFEST_URL);

        return AppVersion.isNewer(manifest.getVersion()) ? manifest : null;
    }

    public void downloadAndApply(
            LauncherUpdateManifest update,
            ProgressListener listener
    ) throws IOException, InterruptedException {

        listener.indeterminate("Mise à jour", "Téléchargement de la version " + update.getVersion() + "...");

        Path installerPath = GameDirectory.getCacheDirectory()
                .resolve("updates")
                .resolve(LauncherConfig.LAUNCHER_INSTALL_NAME + "-" + update.getVersion() + ".exe");

        HttpDownloader.downloadSha256(
                update.getInstallerUrl(),
                installerPath,
                update.getSha256(),
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

        relaunchAndExit();
    }

    private void relaunchAndExit() throws IOException {

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
