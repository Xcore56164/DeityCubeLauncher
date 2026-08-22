package fr.deitycube.launcher.logging;

import fr.deitycube.launcher.filesystem.GameDirectory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class LauncherLogger {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter LINE_TIMESTAMP =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final int MAX_LOG_FILES_PER_PREFIX = 10;

    private static boolean initialized;

    private LauncherLogger() {
    }

    public static synchronized void initialize() {

        if (initialized) {
            return;
        }

        initialized = true;

        try {

            Path logsDirectory = GameDirectory.getLogsDirectory();
            Files.createDirectories(logsDirectory);

            Path logFile = logsDirectory.resolve(
                    "launcher-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".log"
            );

            OutputStream fileStream = new FileOutputStream(logFile.toFile(), true);

            System.setOut(new TeeingPrintStream(System.out, fileStream, "OUT"));
            System.setErr(new TeeingPrintStream(System.err, fileStream, "ERR"));

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("Exception non interceptée sur le thread '" + thread.getName() + "'");
                throwable.printStackTrace(System.err);
            });

            pruneOldLogs(logsDirectory, "launcher-");
            pruneOldLogs(logsDirectory, "game-");

        } catch (IOException e) {
            System.err.println("Impossible d'initialiser les logs du launcher : " + e.getMessage());
        }
    }

    private static void pruneOldLogs(Path logsDirectory, String prefix) {

        try (Stream<Path> files = Files.list(logsDirectory)) {

            List<Path> matching = files
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(Comparator.comparing(LauncherLogger::lastModifiedSafe).reversed())
                    .toList();

            for (int i = MAX_LOG_FILES_PER_PREFIX; i < matching.size(); i++) {
                Files.deleteIfExists(matching.get(i));
            }

        } catch (IOException ignored) {
            // La purge des anciens logs n'est pas critique pour le fonctionnement du launcher.
        }
    }

    private static long lastModifiedSafe(Path path) {

        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static final class TeeingPrintStream extends PrintStream {

        private final PrintStream console;
        private final OutputStream file;
        private final String level;
        private boolean atLineStart = true;

        private TeeingPrintStream(PrintStream console, OutputStream file, String level) {
            super(console, true, StandardCharsets.UTF_8);
            this.console = console;
            this.file = file;
            this.level = level;
        }

        @Override
        public void write(byte[] buf, int off, int len) {

            super.write(buf, off, len);
            writeToFile(new String(buf, off, len, StandardCharsets.UTF_8));
        }

        @Override
        public void write(int b) {

            super.write(b);
            writeToFile(String.valueOf((char) b));
        }

        private void writeToFile(String text) {

            // Les flux out et err partagent le même fichier : on synchronise sur ce fichier
            // (pas sur `this`) pour que leurs écritures ne s'entrelacent jamais.
            synchronized (file) {

                try {

                    for (int i = 0; i < text.length(); i++) {

                        char c = text.charAt(i);

                        if (atLineStart) {
                            String prefix = "[" + LocalDateTime.now().format(LINE_TIMESTAMP) + "] [" + level + "] ";
                            file.write(prefix.getBytes(StandardCharsets.UTF_8));
                            atLineStart = false;
                        }

                        file.write(c);

                        if (c == '\n') {
                            atLineStart = true;
                        }
                    }

                    file.flush();

                } catch (IOException e) {
                    console.println("Écriture du log fichier impossible : " + e.getMessage());
                }
            }
        }
    }
}
