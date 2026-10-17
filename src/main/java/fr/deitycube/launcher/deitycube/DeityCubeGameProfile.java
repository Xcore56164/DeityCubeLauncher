package fr.deitycube.launcher.deitycube;

import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.filesystem.GameDirectory;

import java.nio.file.Path;
import java.util.Map;

/**
 * Ce qu'il faut installer et lancer pour un profil du launcher : manifeste, profil du modpack,
 * dossier de jeu, en-têtes des téléchargements et serveur à rejoindre.
 *
 * Minecraft, NeoForge, bibliothèques et assets restent partagés dans game/ (rangés par
 * version) ; seuls le modpack et les données du joueur (mods, config, mondes, options...) vont
 * dans le dossier de jeu du profil.
 */
public final class DeityCubeGameProfile {

    private final DeityCubeManifest manifest;
    private final String modpackProfile;
    private final Path gameDirectory;
    private final Map<String, String> requestHeaders;
    private final String server;

    private DeityCubeGameProfile(
            DeityCubeManifest manifest,
            String modpackProfile,
            Path gameDirectory,
            Map<String, String> requestHeaders,
            String server
    ) {
        this.manifest = manifest;
        this.modpackProfile = modpackProfile;
        this.gameDirectory = gameDirectory;
        this.requestHeaders = Map.copyOf(requestHeaders);
        this.server = server;
    }

    /** Profil du modpack principal (Max, Light...), dans game/. */
    public static DeityCubeGameProfile main(
            DeityCubeManifest manifest,
            String modpackProfile
    ) {
        return new DeityCubeGameProfile(
                manifest,
                modpackProfile,
                GameDirectory.getGameDirectory(),
                Map.of(),
                null
        );
    }

    /**
     * Profil « Test » : manifeste de test.php, fichiers téléchargés avec le jeton de session,
     * dossier game-test/ (le nettoyage du modpack de test ne touche jamais game/), connexion
     * directe au serveur test.
     */
    public static DeityCubeGameProfile test(
            DeityCubeManifest testManifest,
            String sessionToken
    ) {
        return new DeityCubeGameProfile(
                testManifest,
                LauncherConfig.TEST_MODPACK_PROFILE,
                GameDirectory.getTestGameDirectory(),
                Map.of("Authorization", "Bearer " + sessionToken),
                testManifest.getServer()
        );
    }

    public DeityCubeManifest getManifest() {
        return manifest;
    }

    public String getModpackProfile() {
        return modpackProfile;
    }

    public Path getGameDirectory() {
        return gameDirectory;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    /** Serveur (hôte:port) à rejoindre dès le lancement, ou null. */
    public String getServer() {
        return server;
    }
}
