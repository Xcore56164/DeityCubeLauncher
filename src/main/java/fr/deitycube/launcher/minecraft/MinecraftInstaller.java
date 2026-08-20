package fr.deitycube.launcher.minecraft;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.download.MinecraftDownload;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftInstaller {

    public void installClient(
            MinecraftVersionMetadata metadata
    ) throws IOException {

        MinecraftDownload client =
                metadata.getDownloads().getClient();

        Path versionDirectory =
                GameDirectory.getMinecraftVersionDirectory(
                        metadata.getId()
                );

        Path clientPath =
                versionDirectory.resolve(
                        metadata.getId() + ".jar"
                );

        System.out.println("Client Minecraft :");
        System.out.println("Fichier : " + clientPath);
        System.out.println("Taille attendue : "
                + client.getSize()
                + " octets");


        if (Files.exists(clientPath)) {

            System.out.println(
                    "Client Minecraft déjà présent."
            );

            if (isValid(clientPath, client)) {

                System.out.println(
                        "Client Minecraft déjà valide."
                );
                System.out.println();

                return;
            }

            System.out.println(
                    "Le client existant est invalide."
            );

            Files.delete(clientPath);
        }

        System.out.println(
                "Téléchargement de Minecraft "
                        + metadata.getId()
                        + "..."
        );

        HttpDownloader.downloadSha1(
                client.getUrl(),
                clientPath,
                client.getSha1()
        );

        long actualSize = Files.size(clientPath);

        if (actualSize != client.getSize()) {

            Files.deleteIfExists(clientPath);

            throw new IOException(
                    "La taille du client Minecraft est incorrecte.\n"
                            + "Attendu : "
                            + client.getSize()
                            + "\n"
                            + "Obtenu : "
                            + actualSize
            );
        }

        System.out.println(
                "Client Minecraft installé avec succès."
        );
        System.out.println();
    }

    private boolean isValid(
            Path file,
            MinecraftDownload download
    ) throws IOException {

        long actualSize = Files.size(file);

        if (actualSize != download.getSize()) {

            System.out.println(
                    "Taille incorrecte."
            );

            return false;
        }

        boolean validSha1 =
                HashUtils.verifySha1(
                        file,
                        download.getSha1()
                );

        if (!validSha1) {

            System.out.println(
                    "SHA-1 incorrect."
            );

            return false;
        }

        return true;
    }
}
