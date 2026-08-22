package fr.deitycube.launcher.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {

    public static final String CURRENT = load();

    private AppVersion() {
    }

    private static String load() {

        try (InputStream stream = AppVersion.class.getResourceAsStream("/fr/deitycube/launcher/version.properties")) {

            if (stream == null) {
                return "0.0.0";
            }

            Properties properties = new Properties();
            properties.load(stream);

            String version = properties.getProperty("version");

            return version != null && !version.isBlank() ? version.trim() : "0.0.0";

        } catch (IOException e) {
            return "0.0.0";
        }
    }

    public static boolean isNewer(String remoteVersion) {

        if (remoteVersion == null || remoteVersion.isBlank()) {
            return false;
        }

        int[] current = parseSegments(CURRENT);
        int[] remote = parseSegments(remoteVersion);

        int length = Math.max(current.length, remote.length);

        for (int i = 0; i < length; i++) {

            int currentSegment = i < current.length ? current[i] : 0;
            int remoteSegment = i < remote.length ? remote[i] : 0;

            if (remoteSegment != currentSegment) {
                return remoteSegment > currentSegment;
            }
        }

        return false;
    }

    private static int[] parseSegments(String version) {

        String[] parts = version.trim().split("\\.");
        int[] segments = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {

            String digitsOnly = parts[i].replaceAll("[^0-9]", "");

            segments[i] = digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
        }

        return segments;
    }
}
