package fr.deitycube.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha1(Path file) throws IOException {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-1");

            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            return toHex(digest.digest());

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-1 n'est pas disponible dans cette JVM.",
                    exception
            );
        }
    }

    public static boolean verifySha1(
            Path file,
            String expectedHash
    ) throws IOException {

        if (expectedHash == null
                || expectedHash.isBlank()) {

            throw new IllegalArgumentException(
                    "Le hash SHA-1 attendu ne peut pas être vide."
            );
        }

        String actualHash =
                sha1(file);

        return actualHash.equalsIgnoreCase(
                expectedHash
        );
    }

    public static String sha256(Path file)
            throws IOException {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            return toHex(digest.digest());

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 n'est pas disponible dans cette JVM.",
                    exception
            );
        }
    }

    public static boolean verifySha256(
            Path file,
            String expectedHash
    ) throws IOException {

        if (expectedHash == null
                || expectedHash.isBlank()) {

            throw new IllegalArgumentException(
                    "Le hash SHA-256 attendu ne peut pas être vide."
            );
        }

        String actualHash =
                sha256(file);

        return actualHash.equalsIgnoreCase(
                expectedHash
        );
    }

    private static String toHex(byte[] bytes) {

        StringBuilder result =
                new StringBuilder(
                        bytes.length * 2
                );

        for (byte value : bytes) {

            result.append(
                    String.format(
                            "%02x",
                            value
                    )
            );
        }

        return result.toString();
    }
}
