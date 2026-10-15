package fr.deitycube.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;

/**
 * Un antivirus qui analyse un fichier le garde ouvert un court instant, ce qui fait échouer
 * (sous Windows) un déplacement ou une suppression qui aurait réussi une seconde plus tôt :
 * toutes les opérations sensibles sont donc retentées quelques fois.
 */
final class FileUtil {

    private static final int RETRIES = 15;
    private static final long RETRY_DELAY_MS = 1000;

    private FileUtil() {
    }

    static MessageDigest newSha256() {

        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    static String sha256(Path file) throws IOException {

        MessageDigest digest = newSha256();

        try (InputStream in = Files.newInputStream(file)) {

            byte[] buffer = new byte[64 * 1024];
            int read;

            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    static void deleteRecursively(Path path) throws IOException {

        if (!Files.exists(path)) {
            return;
        }

        IOException last = null;

        for (int attempt = 0; attempt < RETRIES; attempt++) {

            try (Stream<Path> walk = Files.walk(path)) {

                for (Path entry : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(entry);
                }

                return;

            } catch (FileSystemException e) {
                last = e;
                pause();
            }
        }

        throw last;
    }

    /**
     * Supprime ce qui peut l'être, sans jamais attendre : un fichier encore utilisé (un ancien
     * launcher resté ouvert verrouille ses jars sous Windows) est simplement laissé en place et
     * sera retenté au prochain démarrage. À réserver au ménage, jamais à ce dont dépend une
     * installation (voir {@link #deleteRecursively(Path)} pour ça).
     *
     * @return {@code true} si tout a été supprimé
     */
    static boolean deleteBestEffort(Path path) {

        if (!Files.exists(path)) {
            return true;
        }

        boolean complete = true;

        try (Stream<Path> walk = Files.walk(path)) {

            for (Path entry : walk.sorted(Comparator.reverseOrder()).toList()) {

                try {
                    Files.deleteIfExists(entry);
                } catch (IOException e) {
                    complete = false;
                }
            }

        } catch (IOException e) {
            return false;
        }

        return complete;
    }

    static void moveWithRetry(Path source, Path target) throws IOException {

        IOException last = null;

        for (int attempt = 0; attempt < RETRIES; attempt++) {

            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
                return;

            } catch (AtomicMoveNotSupportedException e) {
                Files.move(source, target);
                return;

            } catch (FileSystemException e) {
                last = e;
                pause();
            }
        }

        throw last;
    }

    /** Écrit dans un fichier voisin puis le renomme : un lecteur ne voit jamais un fichier partiel. */
    static void writeAtomic(Path file, String content) throws IOException {

        Files.createDirectories(file.getParent());

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");

        Files.writeString(temporary, content, StandardCharsets.UTF_8);

        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void pause() throws IOException {

        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrompu pendant une nouvelle tentative");
        }
    }
}
