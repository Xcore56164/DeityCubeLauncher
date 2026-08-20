package fr.deitycube.launcher.neoforge;

import java.util.List;

public final class NeoForgeProcessorDefinition {

    private final String jar;
    private final List<String> classpath;
    private final List<String> args;
    private final String mainClass;

    public NeoForgeProcessorDefinition(
            String jar,
            List<String> classpath,
            List<String> args,
            String mainClass
    ) {
        this.jar = jar;
        this.classpath = classpath;
        this.args = args;
        this.mainClass = mainClass;
    }

    public String getJar() {
        return jar;
    }

    public List<String> getClasspath() {
        return classpath;
    }

    public List<String> getArgs() {
        return args;
    }

    public String getMainClass() {
        return mainClass;
    }
}
