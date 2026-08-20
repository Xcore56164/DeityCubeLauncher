package fr.deitycube.launcher.minecraft;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.launch.MinecraftLaunchConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class MinecraftPlaceholderResolver {

    public String resolve(
            String value,
            Map<String, String> placeholders
    ) {

        String result = value;

        for (Map.Entry<String, String> entry :
                placeholders.entrySet()) {

            result = result.replace(
                    "${" + entry.getKey() + "}",
                    entry.getValue()
            );
        }

        return result;
    }

    public void validate(
            String value
    ) {

        if (value.contains("${")) {

            throw new IllegalStateException(
                    "Placeholder Minecraft non résolu : "
                            + value
            );
        }
    }

    public Map<String, String> createPlaceholders(
            MinecraftLaunchConfiguration configuration,
            String classpath
    ) {

        Map<String, String> placeholders =
                new HashMap<>();

        placeholders.put(
                "game_directory",
                configuration
                        .getGameDirectory()
                        .toAbsolutePath()
                        .toString()
        );

        placeholders.put(
                "library_directory",
                GameDirectory
                        .getLibrariesDirectory()
                        .toAbsolutePath()
                        .toString()
        );

        placeholders.put(
                "classpath",
                classpath
        );

        placeholders.put(
                "classpath_separator",
                System.getProperty(
                        "path.separator"
                )
        );

        placeholders.put(
                "version_name",
                configuration.getVersion()
        );

        placeholders.put(
                "assets_root",
                configuration
                        .getAssetsDirectory()
                        .toAbsolutePath()
                        .toString()
        );

        placeholders.put(
                "assets_index_name",
                configuration.getAssetIndex()
        );

        placeholders.put(
                "natives_directory",
                configuration
                        .getNativesDirectory()
                        .toAbsolutePath()
                        .toString()
        );

        return placeholders;
    }
}