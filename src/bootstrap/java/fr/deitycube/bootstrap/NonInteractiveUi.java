package fr.deitycube.bootstrap;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface sans fenêtre. {@code acceptUpdates} sert aux tests de bout en bout (tout accepter,
 * attente réduite) ; sans écran (serveur, SSH), elle refuse tout et laisse simplement lancer la
 * version déjà installée.
 */
final class NonInteractiveUi implements BootstrapUi {

    private final boolean acceptUpdates;

    NonInteractiveUi(boolean acceptUpdates) {
        this.acceptUpdates = acceptUpdates;
    }

    @Override
    public boolean confirmUpdate(String version, String notes, boolean firstInstall) {

        BootstrapLog.info("Mise à jour " + version + (acceptUpdates ? " acceptée" : " ignorée") + " (mode sans fenêtre)");

        return acceptUpdates;
    }

    @Override
    public Progress progress(String title) {

        BootstrapLog.info(title);

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
    public void antivirusWait(int seconds, Path folder) throws InterruptedException {

        BootstrapLog.info("Attente antivirus : " + seconds + " s");

        Thread.sleep(seconds * 1000L);
    }

    @Override
    public boolean askRetry(List<String> problems, Path folder) {

        BootstrapLog.info("Fichiers manquants ou modifiés après extraction : " + problems);

        return false;
    }

    @Override
    public void warn(String title, String message) {
        BootstrapLog.info(title + " : " + message);
    }

    @Override
    public void error(String title, String message) {
        BootstrapLog.info(title + " : " + message);
    }
}
