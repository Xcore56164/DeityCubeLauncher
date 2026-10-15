package fr.deitycube.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ZipExtractor {

    private static final int MAX_ENTRIES = 5_000;
    private static final long MAX_TOTAL_BYTES = 2L << 30;

    private ZipExtractor() {
    }

    interface Progress {
        void update(int entries);
    }

    /**
     * Extrait l'archive dans {@code destination}. Le payload est déjà authentifié par sa
     * signature quand on arrive ici : les contrôles ci-dessous (chemins, nombre et taille des
     * entrées) sont une défense supplémentaire, pas la protection principale.
     */
    static void extract(Path zip, Path destination, Progress progress) throws IOException {

        Path root = destination.toAbsolutePath().normalize();

        Files.createDirectories(root);

        int entries = 0;
        long total = 0;

        try (InputStream in = Files.newInputStream(zip);
             ZipInputStream zipIn = new ZipInputStream(in)) {

            ZipEntry entry;

            while ((entry = zipIn.getNextEntry()) != null) {

                if (++entries > MAX_ENTRIES) {
                    throw new IOException("Archive invalide : trop d'entrées");
                }

                String name = entry.getName();

                if (!PayloadDescriptor.isSafeRelativePath(name.endsWith("/")
                        ? name.substring(0, name.length() - 1)
                        : name)) {
                    throw new IOException("Archive invalide : chemin refusé : " + name);
                }

                Path target = root.resolve(name).normalize();

                if (!target.startsWith(root)) {
                    throw new IOException("Archive invalide : chemin hors du dossier : " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }

                Files.createDirectories(target.getParent());

                try (OutputStream out = Files.newOutputStream(target)) {

                    byte[] buffer = new byte[64 * 1024];
                    int read;

                    while ((read = zipIn.read(buffer)) != -1) {

                        total += read;

                        if (total > MAX_TOTAL_BYTES) {
                            throw new IOException("Archive invalide : taille décompressée excessive");
                        }

                        out.write(buffer, 0, read);
                    }
                }

                progress.update(entries);
            }
        }
    }
}
