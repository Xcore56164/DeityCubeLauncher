package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftArgumentRule {

    private String action;
    private MinecraftArgumentOs os;

    public String getAction() {
        return action;
    }

    public MinecraftArgumentOs getOs() {
        return os;
    }
}
