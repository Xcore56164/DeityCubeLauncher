package fr.deitycube.launcher.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftArgument {

    private List<MinecraftArgumentRule> rules;
    private Object value;

    public List<MinecraftArgumentRule> getRules() {
        return rules;
    }

    public Object getValue() {
        return value;
    }
}
