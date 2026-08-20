package fr.deitycube.launcher.network;

import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HttpDownloader {

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 30_000;

    private HttpDownloader() {
    }

    public static void downloadSha1(
            String url,
            Path destination,
            String expectedSha1
    ) throws IOException {

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Seules les connexions HTTPS sont autorisées : " + url
            );
        }

        if (expectedSha1 == null || expectedSha1.isBlank()) {
            throw new IllegalArgumentException(
                    "Le SHA-1 attendu ne peut pas être vide."
            );
        }

        Path parent = destination.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporaryFile = destination.resolveSibling(
                destination.getFileName() + ".download"
        );

        URL targetUrl = uri.toURL();

        HttpURLConnection connection =
                (HttpURLConnection) targetUrl.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "Téléchargement impossible. HTTP "
                                + responseCode
                                + " : "
                                + url
                );
            }

            try (InputStream inputStream = connection.getInputStream()) {

                Files.copy(
                        inputStream,
                        temporaryFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            System.out.println(
                    "Vérification SHA-1 : "
                            + destination.getFileName()
            );

            String actualSha1 = HashUtils.sha1(temporaryFile);

            if (!actualSha1.equalsIgnoreCase(expectedSha1)) {

                Files.deleteIfExists(temporaryFile);

                throw new IOException(
                        "Échec de vérification SHA-1 pour "
                                + destination.getFileName()
                                + ".\n"
                                + "Attendu : "
                                + expectedSha1
                                + "\n"
                                + "Obtenu  : "
                                + actualSha1
                );
            }

            Files.move(
                    temporaryFile,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

            System.out.println(
                    "Fichier vérifié et installé : "
                            + destination.getFileName()
            );

        } finally {

            connection.disconnect();

            Files.deleteIfExists(temporaryFile);
        }
    }

    public static void downloadSha1(
            String url,
            Path destination
    ) throws IOException {

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Seules les connexions HTTPS sont autorisées : " + url
            );
        }

        Path parent = destination.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporaryFile = destination.resolveSibling(
                destination.getFileName() + ".download"
        );

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);

        try {

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "Téléchargement impossible. HTTP "
                                + responseCode
                                + " : "
                                + url
                );
            }

            try (InputStream inputStream = connection.getInputStream()) {

                Files.copy(
                        inputStream,
                        temporaryFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            Files.move(
                    temporaryFile,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } finally {

            connection.disconnect();

            Files.deleteIfExists(temporaryFile);
        }
    }

    public static boolean verifySha1(
            Path file,
            String expectedSha1
    ) throws IOException {

        if (!Files.exists(file)) {
            return false;
        }

        if (expectedSha1 == null
                || expectedSha1.isBlank()) {
            throw new IllegalArgumentException(
                    "Le SHA-1 attendu ne peut pas être vide."
            );
        }

        String actualSha1 =
                HashUtils.sha1(file);

        return actualSha1.equalsIgnoreCase(
                expectedSha1
        );
    }

    public static boolean verifySha256(
            Path file,
            String expectedSha256
    ) throws IOException {

        if (!Files.exists(file)) {
            return false;
        }

        if (expectedSha256 == null
                || expectedSha256.isBlank()) {
            throw new IllegalArgumentException(
                    "Le SHA-256 attendu ne peut pas être vide."
            );
        }

        String actualSha256 =
                HashUtils.sha256(file);

        return actualSha256.equalsIgnoreCase(
                expectedSha256
        );
    }

    public static String downloadText(
            String url
    ) throws IOException {

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Seules les connexions HTTPS sont autorisées : "
                            + url
            );
        }

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);

        try {

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "Téléchargement impossible. HTTP "
                                + responseCode
                                + " : "
                                + url
                );
            }

            try (InputStream inputStream =
                         connection.getInputStream()) {

                return new String(
                        inputStream.readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8
                ).trim();
            }

        } finally {
            connection.disconnect();
        }
    }

    public static void downloadSha256(
            String url,
            Path destination,
            String expectedSha256
    ) throws IOException {

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme())) {

            throw new IllegalArgumentException(
                    "Seules les connexions HTTPS sont autorisées : "
                            + url
            );
        }

        if (expectedSha256 == null
                || expectedSha256.isBlank()) {

            throw new IllegalArgumentException(
                    "Le SHA-256 attendu ne peut pas être vide."
            );
        }

        Path parent =
                destination.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporaryFile =
                destination.resolveSibling(
                        destination.getFileName()
                                + ".download"
                );

        URL targetUrl =
                uri.toURL();

        HttpURLConnection connection =
                (HttpURLConnection)
                        targetUrl.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(
                CONNECT_TIMEOUT
        );
        connection.setReadTimeout(
                READ_TIMEOUT
        );
        connection.setInstanceFollowRedirects(
                true
        );

        try {

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                throw new IOException(
                        "Téléchargement impossible. HTTP "
                                + responseCode
                                + " : "
                                + url
                );
            }

            try (InputStream inputStream =
                         connection.getInputStream()) {

                Files.copy(
                        inputStream,
                        temporaryFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            System.out.println(
                    "Vérification SHA-256 : "
                            + destination.getFileName()
            );

            String actualSha256 =
                    HashUtils.sha256(
                            temporaryFile
                    );

            if (!actualSha256.equalsIgnoreCase(
                    expectedSha256
            )) {

                Files.deleteIfExists(
                        temporaryFile
                );

                throw new IOException(
                        "Échec de vérification SHA-256 pour "
                                + destination.getFileName()
                                + ".\n"
                                + "Attendu : "
                                + expectedSha256
                                + "\n"
                                + "Obtenu  : "
                                + actualSha256
                );
            }

            Files.move(
                    temporaryFile,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

            System.out.println(
                    "Fichier vérifié et installé : "
                            + destination.getFileName()
            );

        } finally {

            connection.disconnect();

            Files.deleteIfExists(
                    temporaryFile
            );
        }
    }
}
