package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftArgumentOs {

    private String name;
    private String arch;

    public String getName() {
        return name;
    }

    public String getArch() {
        return arch;
    }
}
