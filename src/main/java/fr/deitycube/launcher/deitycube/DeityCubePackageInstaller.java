package fr.deitycube.launcher.deitycube;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.progress.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DeityCubePackageInstaller {

    private final Path gameDirectory;

    public DeityCubePackageInstaller(
            Path gameDirectory
    ) {
        this.gameDirectory = gameDirectory;
    }

    public void install(
            DeityCubePackage pack
    ) throws IOException {

        install(pack, ProgressListener.NONE);
    }

    public void install(
            DeityCubePackage pack,
            ProgressListener listener
    ) throws IOException {

        System.out.println();
        System.out.println(
                "================================="
        );
        System.out.println(
                "       MODPACK DEITYCUBE"
        );
        System.out.println(
                "================================="
        );
        System.out.println(
                "Nom     : "
                        + pack.getName()
        );
        System.out.println(
                "Version : "
                        + pack.getVersion()
        );

        Path archive =
                downloadPackage(
                        pack,
                        listener
                );

        listener.indeterminate(
                "Installation du modpack",
                "Extraction de " + pack.getFilename() + "..."
        );

        extractPackage(
                archive
        );
    }

    private Path downloadPackage(
            DeityCubePackage pack,
            ProgressListener listener
    ) throws IOException {

        Path destination =
                GameDirectory
                        .getCacheDirectory()
                        .resolve("deitycube")
                        .resolve(
                                pack.getFilename()
                        );

        if (Files.isRegularFile(destination)
                && HttpDownloader.verifySha256(
                        destination,
                        pack.getSha256()
                )) {

            System.out.println(
                    "Archive déjà présente et valide : "
                            + destination.getFileName()
            );

            return destination;
        }

        System.out.println(
                "Téléchargement modpack : "
                        + pack.getFilename()
        );

        listener.phase("Téléchargement du modpack");

        HttpDownloader.downloadSha256(
                pack.getDownloadUrl(),
                destination,
                pack.getSha256(),
                bytes -> listener.update(
                        "Téléchargement du modpack",
                        pack.getFilename(),
                        bytes,
                        -1
                )
        );

        return destination;
    }

    private void extractPackage(
            Path archive
    ) throws IOException {

        Set<String> managedPaths =
                new HashSet<>();

        Set<String> managedRoots =
                new HashSet<>();

        int extractedCount = 0;

        try (ZipFile zipFile =
                     new ZipFile(archive.toFile())) {

            String wrapperPrefix =
                    detectWrapperPrefix(zipFile);

            Enumeration<? extends ZipEntry> entries =
                    zipFile.entries();

            while (entries.hasMoreElements()) {

                ZipEntry entry =
                        entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String relativePath =
                        wrapperPrefix != null
                                ? entry.getName().substring(
                                        wrapperPrefix.length()
                                )
                                : entry.getName();

                if (relativePath.isBlank()) {
                    continue;
                }

                boolean playerManaged =
                        isPlayerManaged(relativePath);

                Path destination =
                        gameDirectory.resolve(
                                relativePath
                        );

                if (!playerManaged
                        || !Files.isRegularFile(destination)) {

                    Files.createDirectories(
                            destination.getParent()
                    );

                    try (InputStream input =
                                 zipFile.getInputStream(entry)) {

                        Files.copy(
                                input,
                                destination,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    }

                    extractedCount++;
                }

                managedPaths.add(relativePath);

                if (!playerManaged) {

                    managedRoots.add(
                            topLevelSegment(relativePath)
                    );
                }
            }
        }

        System.out.println(
                "Fichiers installés : "
                        + extractedCount
        );

        pruneOrphans(
                managedRoots,
                managedPaths
        );
    }

    private String detectWrapperPrefix(
            ZipFile zipFile
    ) {

        String wrapperPrefix = null;

        Enumeration<? extends ZipEntry> entries =
                zipFile.entries();

        while (entries.hasMoreElements()) {

            ZipEntry entry =
                    entries.nextElement();

            if (entry.isDirectory()) {
                continue;
            }

            String name =
                    entry.getName();

            int separatorIndex =
                    name.indexOf('/');

            if (separatorIndex < 0) {
                return null;
            }

            String topLevel =
                    name.substring(
                            0,
                            separatorIndex + 1
                    );

            if (wrapperPrefix == null) {

                wrapperPrefix = topLevel;

            } else if (!wrapperPrefix.equals(topLevel)) {

                return null;
            }
        }

        return wrapperPrefix;
    }

    private void pruneOrphans(
            Set<String> managedRoots,
            Set<String> managedPaths
    ) throws IOException {

        for (String root : managedRoots) {

            Path rootDirectory =
                    gameDirectory.resolve(root);

            if (!Files.isDirectory(rootDirectory)) {
                continue;
            }

            List<Path> toDelete =
                    new ArrayList<>();

            try (Stream<Path> walk =
                         Files.walk(rootDirectory)) {

                walk.filter(Files::isRegularFile)
                        .forEach(path -> {

                            String relativePath =
                                    gameDirectory
                                            .relativize(path)
                                            .toString()
                                            .replace('\\', '/');

                            if (!managedPaths.contains(
                                    relativePath
                            )) {

                                toDelete.add(path);
                            }
                        });
            }

            for (Path path : toDelete) {

                System.out.println(
                        "Suppression fichier obsolète : "
                                + gameDirectory.relativize(path)
                );

                Files.delete(path);
            }
        }
    }

    private boolean isPlayerManaged(
            String relativePath
    ) {

        return relativePath.equals("options.txt");
    }

    private String topLevelSegment(
            String relativePath
    ) {

        int separatorIndex =
                relativePath.indexOf('/');

        return separatorIndex < 0
                ? relativePath
                : relativePath.substring(0, separatorIndex);
    }
}
