package fr.deitycube.launcher.deitycube;

import fr.deitycube.launcher.network.HttpDownloader;
import fr.deitycube.launcher.progress.ProgressListener;
import fr.deitycube.launcher.util.HashUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class DeityCubePackageInstaller {

    private final Path gameDirectory;

    public DeityCubePackageInstaller(
            Path gameDirectory
    ) {
        this.gameDirectory = gameDirectory.normalize();
    }

    public void install(
            DeityCubeManifest manifest,
            DeityCubePackage profile
    ) throws IOException {

        install(manifest, profile, ProgressListener.NONE);
    }

    public void install(
            DeityCubeManifest manifest,
            DeityCubePackage profile,
            ProgressListener listener
    ) throws IOException {

        DeityCubeCommonFiles common =
                manifest.getCommon();

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
                        + profile.getName()
        );
        System.out.println(
                "Version : "
                        + profile.getVersion()
        );
        System.out.println(
                "Fichiers communs : "
                        + common.getFiles().size()
        );
        System.out.println(
                "Fichiers du profil : "
                        + profile.getFiles().size()
        );

        Set<String> managedPaths =
                new HashSet<>();

        InstallCounters counters =
                new InstallCounters();

        counters.total =
                common.getFiles().size()
                        + profile.getFiles().size();

        listener.phase("Téléchargement du modpack");

        installGroup(
                common.getBaseUrl(),
                common.getFiles(),
                managedPaths,
                counters,
                listener
        );

        installGroup(
                profile.getBaseUrl(),
                profile.getFiles(),
                managedPaths,
                counters,
                listener
        );

        System.out.println();
        System.out.println(
                "Fichiers téléchargés  : "
                        + counters.downloaded
        );
        System.out.println(
                "Fichiers déjà valides : "
                        + counters.alreadyValid
        );

        listener.indeterminate(
                "Installation du modpack",
                "Nettoyage des fichiers obsolètes..."
        );

        pruneOrphans(
                collectAllRoots(manifest),
                managedPaths
        );
    }

    private void installGroup(
            String baseUrl,
            List<DeityCubePackageFile> files,
            Set<String> managedPaths,
            InstallCounters counters,
            ProgressListener listener
    ) throws IOException {

        for (DeityCubePackageFile file : files) {

            counters.current++;

            String relativePath =
                    file.getPath();

            boolean playerManaged =
                    isPlayerManaged(relativePath);

            Path destination =
                    resolveSafely(relativePath);

            listener.update(
                    "Téléchargement du modpack",
                    relativePath,
                    counters.current,
                    counters.total
            );

            if (playerManaged
                    && Files.isRegularFile(destination)) {

                managedPaths.add(relativePath);
                continue;
            }

            if (isValid(destination, file)) {

                counters.alreadyValid++;

            } else {

                System.out.println(
                        "Téléchargement : "
                                + relativePath
                );

                HttpDownloader.downloadSha256(
                        buildFileUrl(
                                baseUrl,
                                relativePath
                        ),
                        destination,
                        file.getSha256()
                );

                counters.downloaded++;
            }

            managedPaths.add(relativePath);
        }
    }

    private Set<String> collectAllRoots(
            DeityCubeManifest manifest
    ) {

        Set<String> roots =
                new HashSet<>();

        collectRoots(
                manifest.getCommon().getFiles(),
                roots
        );

        for (DeityCubePackage profile :
                manifest.getProfiles().values()) {

            collectRoots(
                    profile.getFiles(),
                    roots
            );
        }

        return roots;
    }

    private void collectRoots(
            List<DeityCubePackageFile> files,
            Set<String> roots
    ) {

        for (DeityCubePackageFile file : files) {

            if (!isPlayerManaged(file.getPath())) {

                roots.add(
                        topLevelSegment(file.getPath())
                );
            }
        }
    }

    private boolean isValid(
            Path file,
            DeityCubePackageFile expected
    ) throws IOException {

        if (!Files.isRegularFile(file)) {
            return false;
        }

        if (expected.getSize() > 0
                && Files.size(file) != expected.getSize()) {

            return false;
        }

        return HashUtils.verifySha256(
                file,
                expected.getSha256()
        );
    }

    private Path resolveSafely(
            String relativePath
    ) throws IOException {

        Path destination =
                gameDirectory.resolve(relativePath)
                        .normalize();

        if (!destination.startsWith(gameDirectory)) {

            throw new IOException(
                    "Chemin de fichier invalide "
                            + "(hors du dossier de jeu) : "
                            + relativePath
            );
        }

        Path parent =
                destination.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        return destination;
    }

    private String buildFileUrl(
            String baseUrl,
            String relativePath
    ) {

        StringBuilder url =
                new StringBuilder(baseUrl);

        if (url.charAt(url.length() - 1) != '/') {
            url.append('/');
        }

        String[] segments =
                relativePath.split("/");

        for (int i = 0; i < segments.length; i++) {

            if (i > 0) {
                url.append('/');
            }

            url.append(
                    URLEncoder.encode(
                                    segments[i],
                                    StandardCharsets.UTF_8
                            )
                            .replace("+", "%20")
            );
        }

        return url.toString();
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

    private static final class InstallCounters {

        private int current;
        private int total;
        private int downloaded;
        private int alreadyValid;
    }
}
