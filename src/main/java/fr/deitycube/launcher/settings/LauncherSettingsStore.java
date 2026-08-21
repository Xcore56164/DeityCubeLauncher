package fr.deitycube.launcher.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.deitycube.launcher.filesystem.GameDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherSettingsStore {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private LauncherSettingsStore() {
    }

    public static LauncherSettings load() {

        Path file = GameDirectory.getSettingsFile();

        if (!Files.isRegularFile(file)) {
            return new LauncherSettings();
        }

        try {
            return OBJECT_MAPPER.readValue(
                    file.toFile(),
                    LauncherSettings.class
            );
        } catch (IOException e) {
            return new LauncherSettings();
        }
    }

    public static void save(
            LauncherSettings settings
    ) throws IOException {

        Path file = GameDirectory.getSettingsFile();

        Files.createDirectories(
                file.getParent()
        );

        OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(
                        file.toFile(),
                        settings
                );
    }
}
