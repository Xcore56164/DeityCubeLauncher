package fr.deitycube.launcher.minecraft.assets;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.MinecraftVersionMetadata;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftAssetManager {

    private final ObjectMapper objectMapper;

    public MinecraftAssetManager(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public MinecraftAssetIndex loadIndex(
            MinecraftVersionMetadata metadata
    ) throws IOException {

        MinecraftAssetIndexInfo assetIndexInfo =
                metadata.getAssetIndex();

        if (assetIndexInfo == null) {
            throw new IOException(
                    "Aucun assetIndex présent dans les metadata Minecraft."
            );
        }

        if (assetIndexInfo.getId() == null
                || assetIndexInfo.getUrl() == null
                || assetIndexInfo.getSha1() == null) {

            throw new IOException(
                    "Les informations de l'asset index sont incomplètes."
            );
        }

        Path indexPath =
                GameDirectory
                        .getAssetsIndexesDirectory()
                        .resolve(
                                assetIndexInfo.getId() + ".json"
                        );

        if (Files.exists(indexPath)) {

            boolean validSize =
                    Files.size(indexPath)
                            == assetIndexInfo.getSize();

            boolean validSha1 =
                    HashUtils.verifySha1(
                            indexPath,
                            assetIndexInfo.getSha1()
                    );

            if (validSize && validSha1) {

                System.out.println(
                        "Asset index "
                                + assetIndexInfo.getId()
                                + " déjà valide."
                );

                return objectMapper.readValue(
                        indexPath.toFile(),
                        MinecraftAssetIndex.class
                );
            }

            System.out.println(
                    "Asset index existant invalide, remplacement..."
            );

            Files.delete(indexPath);
        }

        System.out.println();
        System.out.println(
                "Téléchargement de l'asset index "
                        + assetIndexInfo.getId()
                        + "..."
        );

        HttpDownloader.downloadSha1(
                assetIndexInfo.getUrl(),
                indexPath,
                assetIndexInfo.getSha1()
        );

        long actualSize = Files.size(indexPath);

        if (actualSize != assetIndexInfo.getSize()) {

            Files.deleteIfExists(indexPath);

            throw new IOException(
                    "Taille incorrecte pour l'asset index.\n"
                            + "Attendu : "
                            + assetIndexInfo.getSize()
                            + "\n"
                            + "Obtenu : "
                            + actualSize
            );
        }

        return objectMapper.readValue(
                indexPath.toFile(),
                MinecraftAssetIndex.class
        );
    }
}
