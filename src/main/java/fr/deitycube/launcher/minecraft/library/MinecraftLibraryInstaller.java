package fr.deitycube.launcher.minecraft.library;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.progress.ProgressListener;
import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MinecraftLibraryInstaller {

    public void installLibraries(
            List<MinecraftLibrary> libraries
    ) throws IOException {

        installLibraries(libraries, ProgressListener.NONE);
    }

    public void installLibraries(
            List<MinecraftLibrary> libraries,
            ProgressListener listener
    ) throws IOException {

        int installed = 0;
        int alreadyValid = 0;
        int ignored = 0;
        int current = 0;
        int total = libraries.size();

        for (MinecraftLibrary library : libraries) {

            current++;

            if (!MinecraftLibraryRuleEvaluator.isAllowed(library)) {
                ignored++;
                continue;
            }

            if (library.getDownloads() == null
                    || library.getDownloads().getArtifact() == null) {

                ignored++;
                continue;
            }

            MinecraftLibrary.Artifact artifact =
                    library.getDownloads().getArtifact();

            Path destination =
                    GameDirectory.getLibrariesDirectory()
                            .resolve(artifact.getPath());

            System.out.println();
            System.out.println(
                    "Library : " + library.getName()
            );

            listener.update(
                    "Installation des bibliothèques",
                    library.getName(),
                    current,
                    total
            );

            if (isValid(destination, artifact)) {

                System.out.println(
                        "Déjà valide."
                );

                alreadyValid++;
                continue;
            }

            if (Files.exists(destination)) {

                System.out.println(
                        "Fichier invalide, remplacement..."
                );

                Files.delete(destination);
            }

            System.out.println(
                    "Téléchargement..."
            );

            HttpDownloader.downloadSha1(
                    artifact.getUrl(),
                    destination,
                    artifact.getSha1()
            );

            long actualSize = Files.size(destination);

            if (actualSize != artifact.getSize()) {

                Files.deleteIfExists(destination);

                throw new IOException(
                        "Taille incorrecte pour la library "
                                + library.getName()
                                + ".\n"
                                + "Attendu : "
                                + artifact.getSize()
                                + "\n"
                                + "Obtenu : "
                                + actualSize
                );
            }

            installed++;
        }

        System.out.println();
        System.out.println("=================================");
        System.out.println("        LIBRARIES MINECRAFT");
        System.out.println("=================================");
        System.out.println(
                "Téléchargées : " + installed
        );
        System.out.println(
                "Déjà valides : " + alreadyValid
        );
        System.out.println(
                "Ignorées      : " + ignored
        );
    }

    private boolean isValid(
            Path file,
            MinecraftLibrary.Artifact artifact
    ) throws IOException {

        if (!Files.exists(file)) {
            return false;
        }

        long actualSize = Files.size(file);

        if (actualSize != artifact.getSize()) {
            return false;
        }

        return HashUtils.verifySha1(
                file,
                artifact.getSha1()
        );
    }
}
