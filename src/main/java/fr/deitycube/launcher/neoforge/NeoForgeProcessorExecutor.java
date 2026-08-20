package fr.deitycube.launcher.neoforge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NeoForgeProcessorExecutor {

    private final Path javaExecutable;
    private final Path gameDirectory;
    private final NeoForgePathResolver pathResolver;

    public NeoForgeProcessorExecutor(
            Path javaExecutable,
            Path gameDirectory,
            NeoForgePathResolver pathResolver
    ) {
        this.javaExecutable = javaExecutable;
        this.gameDirectory = gameDirectory;
        this.pathResolver = pathResolver;
    }

    public void execute(
            NeoForgeProcessorDefinition processor
    ) throws IOException, InterruptedException {

        List<String> command =
                new ArrayList<>();

        command.add(
                javaExecutable.toString()
        );

        command.add("-cp");

        command.add(
                buildClasspath(
                        processor.getClasspath()
                )
        );

        command.add(
                processor.getMainClass()
        );

        for (String argument :
                processor.getArgs()) {

            command.add(
                    pathResolver.resolveArgument(argument)
            );
        }

        System.out.println();
        System.out.println(
                "Exécution du processor NeoForge..."
        );

        System.out.println(
                String.join(
                        " ",
                        command
                )
        );

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder
                .directory(
                        gameDirectory.toFile()
                );

        processBuilder
                .inheritIO();

        Process process =
                processBuilder.start();

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IOException(
                    "Le processor NeoForge a échoué "
                            + "avec le code "
                            + exitCode
            );
        }
    }

    private String buildClasspath(
            List<String> entries
    ) {

        List<String> paths =
                new ArrayList<>();

        for (String entry : entries) {

            Path path =
                    pathResolver.resolveMaven(
                            entry
                    );

            paths.add(
                    path.toString()
            );
        }

        return String.join(
                System.getProperty(
                        "path.separator"
                ),
                paths
        );
    }
}
