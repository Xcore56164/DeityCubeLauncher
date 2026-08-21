package fr.deitycube.launcher.minecraft.launch;

import fr.deitycube.launcher.auth.AuthenticationResult;
import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.MinecraftArgumentResolver;
import fr.deitycube.launcher.minecraft.MinecraftPlaceholderResolver;
import fr.deitycube.launcher.minecraft.MinecraftResolvedVersion;
import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;
import fr.deitycube.launcher.minecraft.library.MinecraftLibraryRuleEvaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MinecraftLauncher {

    private static final String LAUNCHER_NAME =
            "DeityCube";

    private static final String LAUNCHER_VERSION =
            LauncherConfig.MODPACK_VERSION;

    public Process launch(
            MinecraftResolvedVersion resolvedVersion,
            AuthenticationResult authentication,
            int allocatedRamMb
    ) throws IOException {

        MinecraftLaunchConfiguration configuration =
                createConfiguration(
                        resolvedVersion
                );

        List<String> command =
                buildCommand(
                        configuration,
                        resolvedVersion,
                        authentication,
                        allocatedRamMb
                );

        printLaunchInformation(
                configuration,
                resolvedVersion
        );

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        command
                );

        processBuilder.directory(
                configuration
                        .getGameDirectory()
                        .toFile()
        );

        Path logFile = createLogFile();

        processBuilder.redirectOutput(
                ProcessBuilder.Redirect.to(logFile.toFile())
        );

        processBuilder.redirectErrorStream(true);

        return processBuilder.start();
    }

    private Path createLogFile() throws IOException {

        Files.createDirectories(
                GameDirectory.getLogsDirectory()
        );

        String fileName =
                "game-"
                        + LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd_HH-mm-ss"
                                )
                        )
                        + ".log";

        return GameDirectory
                .getLogsDirectory()
                .resolve(fileName);
    }

    private MinecraftLaunchConfiguration createConfiguration(
            MinecraftResolvedVersion resolvedVersion
    ) {

        MinecraftArgumentResolver resolver =
                new MinecraftArgumentResolver();

        return new MinecraftLaunchConfiguration(
                resolvedVersion.getId(),
                resolvedVersion.getMainClass(),
                GameDirectory.getGameDirectory(),
                GameDirectory.getAssetsDirectory(),
                GameDirectory.getNativesDirectory(
                        resolvedVersion.getGameVersionId()
                ),
                resolvedVersion
                        .getAssetsMetadata()
                        .getAssetIndex()
                        .getId(),
                resolvedVersion.getJvmArguments(
                        resolver
                ),
                resolvedVersion.getGameArguments(
                        resolver
                )
        );
    }

    private List<String> buildCommand(
            MinecraftLaunchConfiguration configuration,
            MinecraftResolvedVersion resolvedVersion,
            AuthenticationResult authentication,
            int allocatedRamMb
    ) throws IOException {

        String classpath =
                buildClasspath(
                        resolvedVersion
                );

        MinecraftPlaceholderResolver resolver =
                new MinecraftPlaceholderResolver();

        Map<String, String> placeholders =
                resolver.createPlaceholders(
                        configuration,
                        classpath
                );

        addAuthenticationPlaceholders(
                placeholders,
                authentication
        );

        addLauncherPlaceholders(
                placeholders
        );

        List<String> jvmArguments =
                resolveArguments(
                        configuration.getJvmArguments(),
                        resolver,
                        placeholders
                );

        List<String> gameArguments =
                resolveArguments(
                        configuration.getGameArguments(),
                        resolver,
                        placeholders
                );

        List<String> command =
                new ArrayList<>(
                        1
                                + jvmArguments.size()
                                + 1
                                + gameArguments.size()
                );

        command.add(
                getJavaExecutable()
        );

        command.add(
                "-Xmx" + allocatedRamMb + "M"
        );

        command.add(
                "-Xms" + Math.min(allocatedRamMb, 1024) + "M"
        );

        command.addAll(
                jvmArguments
        );

        command.add(
                configuration.getMainClass()
        );

        command.addAll(
                gameArguments
        );

        return command;
    }

    private List<String> resolveArguments(
            List<String> arguments,
            MinecraftPlaceholderResolver resolver,
            Map<String, String> placeholders
    ) {

        List<String> resolved =
                new ArrayList<>(
                        arguments.size()
                );

        for (String argument : arguments) {

            String value =
                    resolver.resolve(
                            argument,
                            placeholders
                    );

            resolver.validate(
                    value
            );

            resolved.add(
                    value
            );
        }

        return resolved;
    }

    private void addAuthenticationPlaceholders(
            Map<String, String> placeholders,
            AuthenticationResult authentication
    ) {

        placeholders.put(
                "auth_player_name",
                authentication.getUsername()
        );

        placeholders.put(
                "auth_uuid",
                authentication.getUuid().toString()
        );

        placeholders.put(
                "auth_access_token",
                authentication.getAccessToken()
        );

        placeholders.put(
                "user_type",
                "legacy"
        );

        placeholders.put(
                "clientid",
                ""
        );

        placeholders.put(
                "auth_xuid",
                ""
        );
    }

    private void addLauncherPlaceholders(
            Map<String, String> placeholders
    ) {

        placeholders.put(
                "launcher_name",
                LAUNCHER_NAME
        );

        placeholders.put(
                "launcher_version",
                LAUNCHER_VERSION
        );

        placeholders.put(
                "version_type",
                "release"
        );
    }

    private String buildClasspath(
            MinecraftResolvedVersion resolvedVersion
    ) throws IOException {

        Path minecraftJar =
                resolvedVersion.getMinecraftJar();

        List<String> entries =
                new ArrayList<>();

        if (minecraftJar != null) {

            if (!Files.isRegularFile(
                    minecraftJar
            )) {

                throw new IOException(
                        "Client Minecraft introuvable : "
                                + minecraftJar
                );
            }

            entries.add(
                    minecraftJar
                            .toAbsolutePath()
                            .toString()
            );
        }

        for (MinecraftLibrary library :
                resolvedVersion.getLibraries()) {

            if (!MinecraftLibraryRuleEvaluator
                    .isAllowed(library)) {

                continue;
            }

            if (library.getDownloads() == null
                    || library.getDownloads()
                    .getArtifact() == null) {

                continue;
            }

            String relativePath =
                    library.getDownloads()
                            .getArtifact()
                            .getPath();

            Path libraryPath =
                    GameDirectory
                            .getLibrariesDirectory()
                            .resolve(
                                    relativePath
                            );

            if (!Files.isRegularFile(
                    libraryPath
            )) {

                throw new IOException(
                        "Library Minecraft introuvable : "
                                + libraryPath
                );
            }

            entries.add(
                    libraryPath
                            .toAbsolutePath()
                            .toString()
            );
        }

        return String.join(
                File.pathSeparator,
                entries
        );
    }

    private String getJavaExecutable() {

        Path javaHome =
                Path.of(
                        System.getProperty(
                                "java.home"
                        )
                );

        boolean windows =
                System.getProperty(
                                "os.name"
                        )
                        .toLowerCase()
                        .contains("win");

        return javaHome
                .resolve("bin")
                .resolve(
                        windows
                                ? "java.exe"
                                : "java"
                )
                .toString();
    }

    private void printLaunchInformation(
            MinecraftLaunchConfiguration configuration,
            MinecraftResolvedVersion resolvedVersion
    ) {

        System.out.println();
        System.out.println(
                "================================="
        );
        System.out.println(
                "       LANCEMENT NEOFORGE"
        );
        System.out.println(
                "================================="
        );

        System.out.println(
                "Minecraft : "
                        + resolvedVersion.getGameVersionId()
        );

        System.out.println(
                "NeoForge  : "
                        + resolvedVersion.getId()
        );

        System.out.println(
                "Client    : "
                        + (resolvedVersion.getMinecraftJar() != null
                                ? resolvedVersion
                                        .getMinecraftJar()
                                        .toAbsolutePath()
                                        .toString()
                                : "(fourni par FML)")
        );

        System.out.println(
                "Main Class: "
                        + configuration.getMainClass()
        );
    }
}