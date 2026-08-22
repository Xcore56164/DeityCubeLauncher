package fr.deitycube.launcher.filesystem;

import fr.deitycube.launcher.minecraft.platform.OperatingSystem;

import java.nio.file.Path;

public final class GameDirectory {

    private static final String APP_FOLDER_NAME = "DeityCube";

    private GameDirectory() {
    }

    public static Path getRootDirectory() {

        return switch (OperatingSystem.current()) {
            case WINDOWS -> windowsRootDirectory();
            case LINUX -> linuxRootDirectory();
            case MACOS -> macRootDirectory();
            case UNKNOWN -> throw new IllegalStateException(
                    "Système d'exploitation non reconnu : impossible de déterminer le dossier de données."
            );
        };
    }

    private static Path windowsRootDirectory() {

        String appData = System.getenv("APPDATA");

        if (appData == null || appData.isBlank()) {
            throw new IllegalStateException(
                    "Impossible de déterminer le dossier AppData de Windows."
            );
        }

        return Path.of(appData, APP_FOLDER_NAME);
    }

    private static Path linuxRootDirectory() {

        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, APP_FOLDER_NAME);
        }

        return Path.of(System.getProperty("user.home"), ".local", "share", APP_FOLDER_NAME);
    }

    private static Path macRootDirectory() {

        return Path.of(
                System.getProperty("user.home"),
                "Library", "Application Support",
                APP_FOLDER_NAME
        );
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

    public static Path getSettingsFile() {
        return getRootDirectory().resolve("settings.json");
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
