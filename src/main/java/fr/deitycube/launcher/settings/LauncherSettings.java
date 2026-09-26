package fr.deitycube.launcher.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.deitycube.launcher.config.LauncherConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LauncherSettings {

    // Les champs des versions précédentes (authMode, offlineUsername, microsoftRefreshToken)
    // sont ignorés à la lecture puis effacés au prochain enregistrement.
    private String deityCompteEmail = "";
    // Jeton de session DeityCompte ("Se souvenir de moi") : révocable depuis le site, il ne
    // donne pas accès au mot de passe ni au compte Microsoft.
    private String deityCompteSession;
    private String selectedProfile = LauncherConfig.DEFAULT_MODPACK_PROFILE;
    private int allocatedRamMb;
    private boolean keepLauncherOpenWhilePlaying = true;
    private boolean rememberMe = true;

    public String getDeityCompteEmail() {
        return deityCompteEmail;
    }

    public void setDeityCompteEmail(String deityCompteEmail) {
        this.deityCompteEmail = deityCompteEmail;
    }

    public String getDeityCompteSession() {
        return deityCompteSession;
    }

    public void setDeityCompteSession(String deityCompteSession) {
        this.deityCompteSession = deityCompteSession;
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public int getAllocatedRamMb() {
        return allocatedRamMb;
    }

    public void setAllocatedRamMb(int allocatedRamMb) {
        this.allocatedRamMb = allocatedRamMb;
    }

    public boolean isKeepLauncherOpenWhilePlaying() {
        return keepLauncherOpenWhilePlaying;
    }

    public void setKeepLauncherOpenWhilePlaying(boolean keepLauncherOpenWhilePlaying) {
        this.keepLauncherOpenWhilePlaying = keepLauncherOpenWhilePlaying;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
