package fr.deitycube.launcher.minecraft.assets;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MinecraftAssetInstaller {

    private static final String ASSET_BASE_URL =
            "https://resources.download.minecraft.net/";

    public void installAssets(
            MinecraftAssetIndex assetIndex
    ) throws IOException {

        int downloaded = 0;
        int alreadyValid = 0;

        int total = assetIndex.getObjects().size();
        int current = 0;

        System.out.println();
        System.out.println("=================================");
        System.out.println("        ASSETS MINECRAFT");
        System.out.println("=================================");
        System.out.println(
                "Assets à vérifier : " + total
        );

        for (Map.Entry<String, MinecraftAsset> entry :
                assetIndex.getObjects().entrySet()) {

            current++;

            MinecraftAsset asset =
                    entry.getValue();

            String hash =
                    asset.getHash();

            long expectedSize =
                    asset.getSize();

            if (hash == null || hash.length() < 2) {

                throw new IOException(
                        "Hash d'asset invalide pour : "
                                + entry.getKey()
                );
            }

            String prefix =
                    hash.substring(0, 2);

            Path destination =
                    GameDirectory
                            .getAssetsObjectsDirectory()
                            .resolve(prefix)
                            .resolve(hash);

            if (isValid(destination, asset)) {

                alreadyValid++;

            } else {

                if (Files.exists(destination)) {
                    Files.delete(destination);
                }

                String url =
                        ASSET_BASE_URL
                                + prefix
                                + "/"
                                + hash;

                HttpDownloader.downloadSha1(
                        url,
                        destination,
                        hash
                );

                long actualSize =
                        Files.size(destination);

                if (actualSize != expectedSize) {

                    Files.deleteIfExists(destination);

                    throw new IOException(
                            "Taille incorrecte pour l'asset : "
                                    + entry.getKey()
                    );
                }

                downloaded++;
            }

            if (current % 100 == 0
                    || current == total) {

                int percentage =
                        (current * 100) / total;

                System.out.println(
                        "Progression : "
                                + percentage
                                + "% ("
                                + current
                                + "/"
                                + total
                                + ")"
                );
            }
        }

        System.out.println();
        System.out.println(
                "Assets téléchargés : "
                        + downloaded
        );

        System.out.println(
                "Assets déjà valides : "
                        + alreadyValid
        );
    }

    private boolean isValid(
            Path file,
            MinecraftAsset asset
    ) throws IOException {

        if (!Files.exists(file)) {
            return false;
        }

        if (Files.size(file) != asset.getSize()) {
            return false;
        }

        return HashUtils.verifySha1(
                file,
                asset.getHash()
        );
    }
}
