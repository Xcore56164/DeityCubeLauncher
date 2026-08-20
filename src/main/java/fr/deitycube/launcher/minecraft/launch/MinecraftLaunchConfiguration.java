package fr.deitycube.launcher.minecraft.launch;

import java.nio.file.Path;
import java.util.List;

public class MinecraftLaunchConfiguration {

    private final String version;
    private final String mainClass;

    private final Path gameDirectory;
    private final Path assetsDirectory;
    private final Path nativesDirectory;

    private final String assetIndex;

    private final List<String> jvmArguments;
    private final List<String> gameArguments;

    public MinecraftLaunchConfiguration(
            String version,
            String mainClass,
            Path gameDirectory,
            Path assetsDirectory,
            Path nativesDirectory,
            String assetIndex,
            List<String> jvmArguments,
            List<String> gameArguments
    ) {
        this.version = version;
        this.mainClass = mainClass;
        this.gameDirectory = gameDirectory;
        this.assetsDirectory = assetsDirectory;
        this.nativesDirectory = nativesDirectory;
        this.assetIndex = assetIndex;
        this.jvmArguments = jvmArguments;
        this.gameArguments = gameArguments;
    }

    public String getVersion() {
        return version;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Path getGameDirectory() {
        return gameDirectory;
    }

    public Path getAssetsDirectory() {
        return assetsDirectory;
    }

    public Path getNativesDirectory() {
        return nativesDirectory;
    }

    public String getAssetIndex() {
        return assetIndex;
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    public List<String> getGameArguments() {
        return gameArguments;
    }
}
