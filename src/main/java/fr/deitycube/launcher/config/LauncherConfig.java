package fr.deitycube.launcher.config;

public final class LauncherConfig {
    private LauncherConfig() {

    }

    public static String MINECRAFT_VERSION;
    public static String NEOFORGE_VERSION;
    public static String MODPACK_VERSION;

    public static final String MINECRAFT_VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static final String DEITYCUBE_MANIFEST_URL =
            "https://deitycube.fr/downloads/manifest_deitycube.json";

    public static final String LAUNCHER_UPDATE_MANIFEST_URL =
            "https://deitycube.fr/downloads/launcher_manifest.json";

    // Doit correspondre au --name utilisé par la tâche Gradle 'createInstaller' (jpackage),
    // puisque c'est le nom du dossier d'installation sous %LOCALAPPDATA%.
    public static final String LAUNCHER_INSTALL_NAME = "DeityCubeLauncher";

    public static final String DEFAULT_MODPACK_PROFILE =
            "Max";
}
