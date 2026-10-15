package fr.deitycube.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journal du bootstrap (le launcher écrit les siens dans le même dossier de logs). Sans effet
 * tant que {@link #init(Path)} n'a pas été appelé, ce qui garde les tests silencieux.
 */
final class BootstrapLog {

    private static final long MAX_SIZE_BYTES = 512 * 1024;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static volatile Path file;

    private BootstrapLog() {
    }

    static void init(Path logsDirectory) {

        try {
            Files.createDirectories(logsDirectory);

            Path target = logsDirectory.resolve("bootstrap.log");

            if (Files.exists(target) && Files.size(target) > MAX_SIZE_BYTES) {
                Files.delete(target);
            }

            file = target;

        } catch (IOException e) {
            file = null;
        }
    }

    static void info(String message) {
        write("INFO ", message);
    }

    static void error(String message, Throwable error) {

        StringWriter trace = new StringWriter();

        if (error != null) {
            error.printStackTrace(new PrintWriter(trace));
        }

        write("ERROR", message + (error != null ? "\n" + trace : ""));
    }

    private static synchronized void write(String level, String message) {

        Path target = file;

        if (target == null) {
            return;
        }

        try {
            Files.writeString(
                    target,
                    LocalDateTime.now().format(FORMAT) + " " + level + " " + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Un journal inutilisable ne doit jamais empêcher de lancer le launcher.
        }
    }
}
