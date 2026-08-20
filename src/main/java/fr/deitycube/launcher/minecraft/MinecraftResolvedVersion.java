package fr.deitycube.launcher.minecraft;

import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinecraftResolvedVersion {

    private final MinecraftVersionMetadata version;
    private final MinecraftVersionMetadata parent;
    private final Path minecraftJar;

    public MinecraftResolvedVersion(
            MinecraftVersionMetadata version,
            MinecraftVersionMetadata parent,
            Path minecraftJar
    ) {
        this.version = version;
        this.parent = parent;
        this.minecraftJar = minecraftJar;
    }

    public MinecraftVersionMetadata getVersion() {
        return version;
    }

    public MinecraftVersionMetadata getParent() {
        return parent;
    }

    public String getId() {
        return version.getId();
    }

    public String getMainClass() {
        return version.getMainClass();
    }

    public Path getMinecraftJar() {
        return minecraftJar;
    }

    public String getGameVersionId() {
        return parent != null
                ? parent.getId()
                : version.getId();
    }

    public MinecraftVersionMetadata getAssetsMetadata() {
        return parent != null
                ? parent
                : version;
    }

    public List<MinecraftLibrary> getLibraries() {

        Map<String, MinecraftLibrary> libraries =
                new LinkedHashMap<>();

        addLibraries(
                libraries,
                parent
        );

        addLibraries(
                libraries,
                version
        );

        return new ArrayList<>(
                libraries.values()
        );
    }

    private void addLibraries(
            Map<String, MinecraftLibrary> target,
            MinecraftVersionMetadata metadata
    ) {

        if (metadata == null
                || metadata.getLibraries() == null) {

            return;
        }

        for (MinecraftLibrary library :
                metadata.getLibraries()) {

            target.put(
                    getLibraryKey(library),
                    library
            );
        }
    }

    private String getLibraryKey(
            MinecraftLibrary library
    ) {

        if (library.getDownloads() != null
                && library.getDownloads().getArtifact() != null
                && library.getDownloads()
                .getArtifact()
                .getPath() != null) {

            return library.getDownloads()
                    .getArtifact()
                    .getPath();
        }

        return library.getName();
    }

    public List<String> getJvmArguments(
            MinecraftArgumentResolver resolver
    ) {

        return resolveArguments(
                resolver,
                true
        );
    }

    public List<String> getGameArguments(
            MinecraftArgumentResolver resolver
    ) {

        return resolveArguments(
                resolver,
                false
        );
    }

    private List<String> resolveArguments(
            MinecraftArgumentResolver resolver,
            boolean jvm
    ) {

        List<String> arguments =
                new ArrayList<>();

        addArguments(
                arguments,
                resolver,
                parent,
                jvm
        );

        addArguments(
                arguments,
                resolver,
                version,
                jvm
        );

        return arguments;
    }

    private void addArguments(
            List<String> target,
            MinecraftArgumentResolver resolver,
            MinecraftVersionMetadata metadata,
            boolean jvm
    ) {

        if (metadata == null
                || metadata.getArguments() == null) {

            return;
        }

        List<Object> arguments =
                jvm
                        ? metadata.getArguments().getJvm()
                        : metadata.getArguments().getGame();

        target.addAll(
                resolver.resolve(
                        arguments
                )
        );
    }
}