package fr.deitycube.launcher.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.deitycube.launcher.config.LauncherConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LauncherSettings {

    private String authMode = "OFFLINE";
    private String offlineUsername = "";
    private String microsoftRefreshToken;
    private String selectedProfile = LauncherConfig.DEFAULT_MODPACK_PROFILE;
    private int allocatedRamMb;
    private boolean closeLauncherOnPlay = false;
    private boolean rememberMe = true;

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getOfflineUsername() {
        return offlineUsername;
    }

    public void setOfflineUsername(String offlineUsername) {
        this.offlineUsername = offlineUsername;
    }

    public String getMicrosoftRefreshToken() {
        return microsoftRefreshToken;
    }

    public void setMicrosoftRefreshToken(String microsoftRefreshToken) {
        this.microsoftRefreshToken = microsoftRefreshToken;
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

    public boolean isCloseLauncherOnPlay() {
        return closeLauncherOnPlay;
    }

    public void setCloseLauncherOnPlay(boolean closeLauncherOnPlay) {
        this.closeLauncherOnPlay = closeLauncherOnPlay;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
