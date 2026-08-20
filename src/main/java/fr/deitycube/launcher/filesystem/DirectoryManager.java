package fr.deitycube.launcher.filesystem;

import fr.deitycube.launcher.config.LauncherConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryManager {

    private DirectoryManager() {
    }


    public static void initialize() throws IOException {
        createDirectory(GameDirectory.getRootDirectory());
        createDirectory(GameDirectory.getGameDirectory());

        createDirectory(GameDirectory.getLibrariesDirectory());
        createDirectory(GameDirectory.getAssetsDirectory());
        createDirectory(GameDirectory.getVersionsDirectory());
        createDirectory(GameDirectory.getDeityCubeDirectory());

        createDirectory(GameDirectory.getCacheDirectory());
        createDirectory(GameDirectory.getLogsDirectory());

        createDirectory(GameDirectory.getMinecraftVersionDirectory(
                LauncherConfig.MINECRAFT_VERSION
        ));

        createDirectory(
                GameDirectory.getNativesDirectory(
                        LauncherConfig.MINECRAFT_VERSION
                )
        );
    }

    private static void createDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
    }
}
