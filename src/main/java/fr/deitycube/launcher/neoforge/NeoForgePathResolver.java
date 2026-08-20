package fr.deitycube.launcher.neoforge;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class NeoForgePathResolver {

    private final Path gameDirectory;
    private final Path librariesDirectory;
    private final Map<String, String> variables = new HashMap<>();

    private final Map<String, Path> paths =
            new HashMap<>();

    public NeoForgePathResolver(
            Path gameDirectory,
            Path minecraftJar
    ) {
        this.gameDirectory = gameDirectory;

        this.librariesDirectory =
                gameDirectory.resolve("libraries");

        paths.put(
                "ROOT",
                gameDirectory
        );

        paths.put(
                "LIBRARY_DIR",
                librariesDirectory
        );

        paths.put(
                "MINECRAFT_JAR",
                minecraftJar
        );

        variables.put("SIDE", "client");
    }

    public void registerVariable(String name, String value) {
        variables.put(name, value);
    }

    public String resolveString(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Valeur à résoudre nulle."
            );
        }

        String result = value;

        for (Map.Entry<String, Path> entry : paths.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue().toString()
            );
        }

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue()
            );
        }

        return result;
    }

    public void register(
            String name,
            Path path
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Nom de chemin invalide."
            );
        }

        if (path == null) {
            throw new IllegalArgumentException(
                    "Chemin nul pour : "
                            + name
            );
        }

        paths.put(name, path);
    }

    public Path resolvePlaceholder(
            String placeholder
    ) {

        if (placeholder == null
                || placeholder.isBlank()) {

            throw new IllegalArgumentException(
                    "Placeholder vide."
            );
        }

        String name =
                placeholder;

        if (name.startsWith("{")
                && name.endsWith("}")) {

            name = name.substring(
                    1,
                    name.length() - 1
            );
        }

        Path path =
                paths.get(name);

        if (path == null) {

            throw new IllegalArgumentException(
                    "Placeholder inconnu : "
                            + placeholder
            );
        }

        return path;
    }

    public Path resolveMaven(
            String coordinate
    ) {

        MavenArtifact artifact =
                MavenArtifact.parse(coordinate);

        return librariesDirectory.resolve(
                artifact.getRelativePath()
        );
    }

    public Path resolveReference(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Référence vide."
            );
        }

        if (value.startsWith("{")
                && value.endsWith("}")) {

            return resolvePlaceholder(value);
        }

        if (value.startsWith("[")
                && value.endsWith("]")) {

            return resolveMaven(value);
        }

        throw new IllegalArgumentException(
                "Référence NeoForge non supportée : "
                        + value
        );
    }

    public String resolveArgument(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Argument nul."
            );
        }

        if (value.startsWith("[")
                && value.endsWith("]")) {

            return resolveMaven(value).toString();
        }

        return resolveString(value);
    }
}