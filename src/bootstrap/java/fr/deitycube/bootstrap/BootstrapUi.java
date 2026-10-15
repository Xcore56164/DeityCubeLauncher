package fr.deitycube.bootstrap;

import java.nio.file.Path;
import java.util.List;

/** Tout ce que le bootstrap montre ou demande à l'utilisateur. */
interface BootstrapUi {

    /**
     * @param firstInstall {@code true} quand aucune version n'est installée : l'utilisateur ne
     *                     peut alors que continuer ou quitter, pas « repousser » la mise à jour
     * @return {@code true} pour installer maintenant
     */
    boolean confirmUpdate(String version, String notes, boolean firstInstall);

    Progress progress(String title);

    /** Laisse le temps à l'antivirus d'analyser les fichiers qui viennent d'être écrits. */
    void antivirusWait(int seconds, Path folder) throws InterruptedException;

    /**
     * Des fichiers ont disparu ou changé juste après leur extraction (en pratique : un
     * antivirus). Indique le dossier à exclure et demande s'il faut réessayer.
     */
    boolean askRetry(List<String> problems, Path folder);

    void warn(String title, String message);

    void error(String title, String message);

    interface Progress extends AutoCloseable {

        void update(String message, long done, long total);

        @Override
        void close();
    }
}
