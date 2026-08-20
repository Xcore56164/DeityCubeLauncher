package fr.deitycube.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.auth.AuthenticationResult;
import fr.deitycube.launcher.auth.OfflineAuthenticator;
import fr.deitycube.launcher.config.LauncherConfig;
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
import fr.deitycube.launcher.minecraft.launch.MinecraftLauncher;
import fr.deitycube.launcher.minecraft.library.MinecraftLibraryInstaller;
import fr.deitycube.launcher.minecraft.natives.NativeInstaller;
import fr.deitycube.launcher.neoforge.NeoForgeInstaller;
import fr.deitycube.launcher.neoforge.NeoForgeInstallerProfile;
import fr.deitycube.launcher.neoforge.NeoForgePathResolver;
import fr.deitycube.launcher.neoforge.NeoForgeProcessor;
import fr.deitycube.launcher.neoforge.NeoForgeVersionInstaller;
import fr.deitycube.launcher.network.HttpDownloader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Launcher {

    private static final String OFFLINE_USERNAME =
            "DeityCubeTest";

    private Launcher() {
    }

    public static void main(String[] args)
            throws Exception {

        printHeader();

        DirectoryManager.initialize();

        MinecraftVersionMetadata minecraft =
                installMinecraft();

        Path neoForgeVersionDirectory =
                installNeoForge(
                        minecraft
                );

        MinecraftResolvedVersion resolvedVersion =
                resolveNeoForge(
                        neoForgeVersionDirectory
                );

        AuthenticationResult authentication =
                OfflineAuthenticator.authenticate(
                        OFFLINE_USERNAME
                );

        new MinecraftLauncher().launch(
                resolvedVersion,
                authentication
        );
    }

    private static MinecraftVersionMetadata installMinecraft()
            throws Exception {

        MinecraftVersionManager versionManager =
                new MinecraftVersionManager();

        Path manifest =
                GameDirectory
                        .getCacheDirectory()
                        .resolve(
                                "minecraft-version-manifest.json"
                        );

        HttpDownloader.downloadSha1(
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
                        metadata
                );

        new MinecraftLibraryInstaller()
                .installLibraries(
                        metadata.getLibraries()
                );

        new NativeInstaller()
                .installNatives(
                        metadata.getLibraries(),
                        metadata.getId()
                );

        installAssets(
                metadata
        );

        return metadata;
    }

    private static void installAssets(
            MinecraftVersionMetadata metadata
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
                        assetIndex
                );
    }

    private static Path installNeoForge(
            MinecraftVersionMetadata minecraft
    ) throws Exception {

        Path gameDirectory =
                GameDirectory.getGameDirectory();

        NeoForgeInstaller installer =
                new NeoForgeInstaller(
                        gameDirectory
                );

        Path installerJar =
                installer.downloadInstaller();

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

        processor.prepare();

        return new NeoForgeVersionInstaller(
                gameDirectory
        ).install(
                profile
        );
    }

    private static MinecraftResolvedVersion resolveNeoForge(
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

    private static Path getMinecraftJar(
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

    private static void printHeader() {

        System.out.println(
                "================================="
        );

        System.out.println(
                "       DEITYCUBE LAUNCHER"
        );

        System.out.println(
                "================================="
        );

        System.out.println(
                "Minecraft : "
                        + LauncherConfig.MINECRAFT_VERSION
        );

        System.out.println(
                "NeoForge  : "
                        + LauncherConfig.NEOFORGE_VERSION
        );

        System.out.println();
    }
}