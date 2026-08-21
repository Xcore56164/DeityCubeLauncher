package fr.deitycube.launcher.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.deitycube.DeityCubeManifest;
import fr.deitycube.launcher.deitycube.DeityCubeManifestReader;
import fr.deitycube.launcher.deitycube.DeityCubePackage;
import fr.deitycube.launcher.deitycube.DeityCubePackageInstaller;
import fr.deitycube.launcher.filesystem.DirectoryManager;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.MinecraftInstaller;
import fr.deitycube.launcher.minecraft.MinecraftMetadataManager;
import fr.deitycube.launcher.minecraft.MinecraftResolvedVersion;
import fr.deitycube.launcher.minecraft.MinecraftVersion;
import fr.deitycube.launcher.minecraft.MinecraftVersionManager;
import fr.deitycube.launcher.minecraft.MinecraftVersionMetadata;
import fr.deitycube.launcher.minecraft.MinecraftVersionResolver;
import fr.deitycube.launcher.minecraft.assets.MinecraftAssetIndex;
import fr.deitycube.launcher.minecraft.assets.MinecraftAssetInstaller;
import fr.deitycube.launcher.minecraft.assets.MinecraftAssetManager;
import fr.deitycube.launcher.minecraft.library.MinecraftLibraryInstaller;
import fr.deitycube.launcher.minecraft.natives.NativeInstaller;
import fr.deitycube.launcher.neoforge.NeoForgeInstaller;
import fr.deitycube.launcher.neoforge.NeoForgeInstallerProfile;
import fr.deitycube.launcher.neoforge.NeoForgePathResolver;
import fr.deitycube.launcher.neoforge.NeoForgeProcessor;
import fr.deitycube.launcher.neoforge.NeoForgeVersionInstaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class InstallationPipeline {

    public static DeityCubeManifest fetchManifest() throws IOException {

        DeityCubeManifest manifest =
                DeityCubeManifestReader.read(
                        LauncherConfig.DEITYCUBE_MANIFEST_URL
                );

        LauncherConfig.MINECRAFT_VERSION =
                manifest.getMinecraftVersion();

        LauncherConfig.NEOFORGE_VERSION =
                manifest.getNeoforgeVersion();

        LauncherConfig.MODPACK_VERSION =
                manifest.getModpackVersion();

        return manifest;
    }

    public MinecraftResolvedVersion install(
            DeityCubeManifest manifest,
            String profileName,
            ProgressListener listener
    ) throws Exception {

        listener.indeterminate(
                "Préparation",
                "Création des dossiers du launcher..."
        );

        DirectoryManager.initialize();

        MinecraftVersionMetadata minecraft =
                installMinecraft(listener);

        Path neoForgeVersionDirectory =
                installNeoForge(minecraft, listener);

        installModpack(manifest, profileName, listener);

        listener.indeterminate(
                "Préparation",
                "Résolution de la version NeoForge..."
        );

        return resolveNeoForge(neoForgeVersionDirectory);
    }

    public MinecraftResolvedVersion reinstall(
            DeityCubeManifest manifest,
            String profileName,
            ProgressListener listener
    ) throws Exception {

        listener.indeterminate(
                "Réinstallation",
                "Suppression des fichiers existants..."
        );

        deleteRecursively(GameDirectory.getVersionsDirectory());
        deleteRecursively(GameDirectory.getLibrariesDirectory());
        deleteRecursively(GameDirectory.getGameDirectory().resolve("natives"));
        deleteRecursively(GameDirectory.getDeityCubeDirectory());
        deleteRecursively(GameDirectory.getCacheDirectory().resolve("deitycube"));
        deleteRecursively(GameDirectory.getCacheDirectory().resolve("neoforge"));

        return install(manifest, profileName, listener);
    }

    private void deleteRecursively(Path directory) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(directory)) {

            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private MinecraftVersionMetadata installMinecraft(
            ProgressListener listener
    ) throws Exception {

        MinecraftVersionManager versionManager =
                new MinecraftVersionManager();

        Path manifest =
                GameDirectory
                        .getCacheDirectory()
                        .resolve(
                                "minecraft-version-manifest.json"
                        );

        listener.indeterminate(
                "Minecraft",
                "Récupération du manifest de versions..."
        );

        fr.deitycube.launcher.network.HttpDownloader.downloadSha1(
                LauncherConfig
                        .MINECRAFT_VERSION_MANIFEST_URL,
                manifest
        );

        MinecraftVersion version =
                versionManager.findTargetVersion(
                        manifest
                );

        Path versionDirectory =
                GameDirectory
                        .getMinecraftVersionDirectory(
                                version.getId()
                        );

        Path metadataPath =
                versionDirectory.resolve(
                        version.getId() + ".json"
                );

        versionManager.downloadMetadata(
                version,
                metadataPath
        );

        MinecraftMetadataManager metadataManager =
                new MinecraftMetadataManager();

        MinecraftVersionMetadata metadata =
                metadataManager.load(
                        metadataPath
                );

        new MinecraftInstaller()
                .installClient(
                        metadata,
                        listener
                );

        new MinecraftLibraryInstaller()
                .installLibraries(
                        metadata.getLibraries(),
                        listener
                );

        new NativeInstaller()
                .installNatives(
                        metadata.getLibraries(),
                        metadata.getId(),
                        listener
                );

        installAssets(
                metadata,
                listener
        );

        return metadata;
    }

    private void installAssets(
            MinecraftVersionMetadata metadata,
            ProgressListener listener
    ) throws Exception {

        ObjectMapper mapper =
                new ObjectMapper();

        MinecraftAssetManager assetManager =
                new MinecraftAssetManager(
                        mapper
                );

        MinecraftAssetIndex assetIndex =
                assetManager.loadIndex(
                        metadata
                );

        new MinecraftAssetInstaller()
                .installAssets(
                        assetIndex,
                        listener
                );
    }

    private Path installNeoForge(
            MinecraftVersionMetadata minecraft,
            ProgressListener listener
    ) throws Exception {

        Path gameDirectory =
                GameDirectory.getGameDirectory();

        NeoForgeInstaller installer =
                new NeoForgeInstaller(
                        gameDirectory
                );

        Path installerJar =
                installer.downloadInstaller(listener);

        NeoForgeInstallerProfile profile =
                installer.readInstallerProfile(
                        installerJar
                );

        Path minecraftJar =
                getMinecraftJar(
                        minecraft
                );

        NeoForgePathResolver pathResolver =
                new NeoForgePathResolver(
                        gameDirectory,
                        minecraftJar
                );

        NeoForgeProcessor processor =
                new NeoForgeProcessor(
                        gameDirectory,
                        minecraftJar,
                        installerJar,
                        profile,
                        pathResolver
                );

        processor.prepare(listener);

        return new NeoForgeVersionInstaller(
                gameDirectory
        ).install(
                profile
        );
    }

    private void installModpack(
            DeityCubeManifest manifest,
            String profileName,
            ProgressListener listener
    ) throws Exception {

        DeityCubePackage pack =
                selectModpackProfile(
                        manifest,
                        profileName
                );

        new DeityCubePackageInstaller(
                GameDirectory.getGameDirectory()
        ).install(
                pack,
                listener
        );
    }

    private DeityCubePackage selectModpackProfile(
            DeityCubeManifest manifest,
            String profileName
    ) {

        for (var entry : manifest.getPackages().entrySet()) {

            if (entry.getKey().equalsIgnoreCase(
                    profileName
            )) {

                return entry.getValue();
            }
        }

        throw new IllegalStateException(
                "Profil de modpack '"
                        + profileName
                        + "' introuvable dans le manifest DeityCube. "
                        + "Profils disponibles : "
                        + manifest.getPackages().keySet()
        );
    }

    private MinecraftResolvedVersion resolveNeoForge(
            Path versionDirectory
    ) throws Exception {

        String versionId =
                versionDirectory
                        .getFileName()
                        .toString();

        Path versionJson =
                versionDirectory.resolve(
                        versionId + ".json"
                );

        if (!Files.isRegularFile(
                versionJson
        )) {

            throw new IllegalStateException(
                    "Version NeoForge introuvable : "
                            + versionJson
            );
        }

        return new MinecraftVersionResolver()
                .loadResolved(
                        versionJson,
                        null
                );
    }

    private Path getMinecraftJar(
            MinecraftVersionMetadata minecraft
    ) {

        return GameDirectory
                .getMinecraftVersionDirectory(
                        minecraft.getId()
                )
                .resolve(
                        minecraft.getId() + ".jar"
                );
    }
}
