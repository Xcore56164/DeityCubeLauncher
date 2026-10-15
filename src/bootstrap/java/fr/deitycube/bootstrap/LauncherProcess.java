package fr.deitycube.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lance le launcher dans un processus à part, avec le runtime installé avec le bootstrap
 * (c'est aussi lui que le launcher utilise ensuite pour Minecraft et NeoForge : il se retrouve
 * via {@code java.home}, comme avant).
 */
final class LauncherProcess {

    private LauncherProcess() {
    }

    static Process start(Path versionDirectory, PayloadDescriptor descriptor, Path logFile) throws IOException {

        List<String> command = new ArrayList<>();

        command.add(javaExecutable(Path.of(System.getProperty("java.home"))).toString());
        command.addAll(descriptor.jvmArgs());
        command.add("-cp");
        command.add(versionDirectory.resolve("lib").toAbsolutePath() + File.separator + "*");
        command.add(descriptor.mainClass());

        Files.createDirectories(logFile.getParent());

        ProcessBuilder builder = new ProcessBuilder(command);

        builder.directory(versionDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));

        BootstrapLog.info("Lancement : " + String.join(" ", command));

        return builder.start();
    }

    /** javaw (sans console) sous Windows, pour qu'aucune fenêtre noire n'accompagne le launcher. */
    static Path javaExecutable(Path javaHome) {

        Path bin = javaHome.resolve("bin");

        if (Os.current() == Os.WINDOWS) {

            Path javaw = bin.resolve("javaw.exe");

            return Files.isRegularFile(javaw) ? javaw : bin.resolve("java.exe");
        }

        return bin.resolve("java");
    }
}
