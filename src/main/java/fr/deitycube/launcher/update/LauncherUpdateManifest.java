package fr.deitycube.launcher.update;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LauncherUpdateManifest {

    private String version;

    private String notes;

    private final Map<String, LauncherUpdateTarget> targets = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public String getNotes() {
        return notes;
    }

    // Toute clé du JSON autre que "version"/"notes" (ex: "windows", "linux_deb", "linux_rpm")
    // est collectée ici, sans avoir à faire évoluer ce POJO à chaque nouvelle plateforme.
    @JsonAnySetter
    public void setTarget(String platformKey, LauncherUpdateTarget target) {
        targets.put(platformKey, target);
    }

    public LauncherUpdateTarget getTarget(String platformKey) {
        return targets.get(platformKey);
    }
}
