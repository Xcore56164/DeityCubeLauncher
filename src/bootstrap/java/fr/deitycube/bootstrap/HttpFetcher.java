package fr.deitycube.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HttpURLConnection plutôt que java.net.http : il applique un délai à chaque lecture, ce qui
 * évite de rester bloqué indéfiniment sur une connexion qui se fige en plein téléchargement.
 */
final class HttpFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final int MAX_TEXT_BYTES = 1024 * 1024;
    private static final long MAX_DOWNLOAD_BYTES = 1L << 30;

    interface Progress {
        void update(long done, long total);
    }

    String getText(URI uri) throws IOException {

        HttpURLConnection connection = open(uri);

        try {
            requireOk(connection, uri);

            try (InputStream in = connection.getInputStream()) {
                return new String(in.readNBytes(MAX_TEXT_BYTES), StandardCharsets.UTF_8);
            }

        } finally {
            connection.disconnect();
        }
    }

    /** Télécharge vers {@code destination} et renvoie le SHA-256 (hexadécimal) du contenu reçu. */
    String download(URI uri, Path destination, Progress progress) throws IOException {

        Files.createDirectories(destination.getParent());

        HttpURLConnection connection = open(uri);

        try {
            requireOk(connection, uri);

            long total = connection.getContentLengthLong();
            MessageDigest digest = FileUtil.newSha256();

            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(destination)) {

                byte[] buffer = new byte[64 * 1024];
                long done = 0;
                int read;

                while ((read = in.read(buffer)) != -1) {

                    done += read;

                    if (done > MAX_DOWNLOAD_BYTES) {
                        throw new IOException("Téléchargement trop volumineux : " + uri);
                    }

                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                    progress.update(done, total);
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (IOException e) {
            Files.deleteIfExists(destination);
            throw e;

        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(URI uri) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "DeityCubeBootstrap/1");

        return connection;
    }

    private void requireOk(HttpURLConnection connection, URI uri) throws IOException {

        int code = connection.getResponseCode();

        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + code + " : " + uri);
        }
    }
}
