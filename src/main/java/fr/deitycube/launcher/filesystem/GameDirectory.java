package fr.deitycube.launcher.filesystem;

import java.nio.file.Path;

public final class GameDirectory {

    private GameDirectory() {
    }

    public static Path getRootDirectory() {
        String appData = System.getenv("APPDATA");

        if (appData == null || appData.isBlank()) {
            throw new IllegalStateException(
                    "Impossible de déterminer le dossier AppData de Windows."
            );
        }

        return Path.of(appData, "DeityCube");
    }

    public static Path getGameDirectory() {
        return getRootDirectory().resolve("game");
    }

    public static Path getLibrariesDirectory() {
        return getGameDirectory().resolve("libraries");
    }

    public static Path getAssetsDirectory() {
        return getGameDirectory().resolve("assets");
    }

    public static Path getVersionsDirectory() {
        return getGameDirectory().resolve("versions");
    }

    public static Path getDeityCubeDirectory() {
        return getGameDirectory().resolve("deitycube");
    }

    public static Path getCacheDirectory() {
        return getRootDirectory().resolve("cache");
    }

    public static Path getLogsDirectory() {
        return getRootDirectory().resolve("logs");
    }

    public static Path getMinecraftVersionDirectory(String version) {
        return getVersionsDirectory().resolve(version);
    }

    public static Path getNativesDirectory(String version) {
        return getGameDirectory()
                .resolve("natives")
                .resolve(version);
    }

    public static Path getAssetsIndexesDirectory() {
        return getAssetsDirectory()
                .resolve("indexes");
    }

    public static Path getAssetsObjectsDirectory() {
        return getAssetsDirectory()
                .resolve("objects");
    }
}
