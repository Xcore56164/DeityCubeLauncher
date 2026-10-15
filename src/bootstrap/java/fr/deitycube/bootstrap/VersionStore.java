package fr.deitycube.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Disposition de {@code <données>/launcher/} (CONTRAT FIGÉ) :
 * <pre>
 *   versions/&lt;v&gt;/   payload extrait (payload.json + lib/)
 *   staging/         téléchargements et extractions en cours
 *   current          version active          (fichier texte)
 *   previous         version de repli         (fichier texte)
 *   blocked          version qui a planté     (fichier texte), plus proposée
 * </pre>
 * Une nouvelle version n'est jamais écrite à la place de l'ancienne : elle est préparée dans
 * staging/, vérifiée, puis déplacée d'un bloc ; seule l'écriture (atomique) du pointeur
 * {@code current} la rend active. Une interruption à n'importe quel moment laisse donc l'ancienne
 * version intacte et utilisable.
 */
final class VersionStore {

    private final Path directory;

    VersionStore(Path launcherDirectory) {
        this.directory = launcherDirectory;
    }

    /** Dossier à exclure de l'antivirus : il contient tout ce que le bootstrap écrit. */
    Path launcherDirectory() {
        return directory;
    }

    Path versionsDirectory() {
        return directory.resolve("versions");
    }

    Path stagingDirectory() {
        return directory.resolve("staging");
    }

    Path versionDirectory(String version) {
        return versionsDirectory().resolve(version);
    }

    Optional<String> current() {
        return readPointer("current");
    }

    Optional<String> previous() {
        return readPointer("previous");
    }

    Optional<String> blocked() {
        return readPointer("blocked");
    }

    /** Version active, seulement si tous ses fichiers sont présents (sinon : à réinstaller). */
    Optional<PayloadDescriptor> intactCurrent() {
        return current().flatMap(this::intact);
    }

    Optional<PayloadDescriptor> intact(String version) {

        Path dir = versionDirectory(version);

        try {
            PayloadDescriptor descriptor = PayloadDescriptor.read(dir);

            if (!descriptor.version().equals(version) || !descriptor.missingFiles(dir).isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(descriptor);

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Rend {@code stagedDirectory} active en tant que {@code version}. */
    void activate(String version, Path stagedDirectory) throws IOException {

        Files.createDirectories(versionsDirectory());

        Path target = versionDirectory(version);

        // Réinstallation d'une version déjà présente (réparation) : l'ancien contenu est cassé.
        FileUtil.deleteRecursively(target);
        FileUtil.moveWithRetry(stagedDirectory, target);

        Optional<String> old = current();

        if (old.isPresent() && !old.get().equals(version)) {
            writePointer("previous", old.get());
        }

        writePointer("current", version);

        blocked().ifPresent(blockedVersion -> {
            if (Versions.compare(version, blockedVersion) > 0) {
                deletePointer("blocked");
            }
        });
    }

    /**
     * Abandonne {@code failedVersion} au profit de la version précédente.
     *
     * @return la version rétablie, ou vide s'il n'y a pas de repli utilisable
     */
    Optional<String> rollback(String failedVersion) throws IOException {

        Optional<String> fallback = previous().filter(version -> intact(version).isPresent());

        writePointer("blocked", failedVersion);

        if (fallback.isEmpty()) {
            return Optional.empty();
        }

        writePointer("current", fallback.get());
        deletePointer("previous");

        return fallback;
    }

    /** Supprime les versions qui ne sont ni active ni de repli, et les restes de staging. */
    void cleanup() throws IOException {

        Set<String> keep = new HashSet<>();

        current().ifPresent(keep::add);
        previous().ifPresent(keep::add);

        if (Files.isDirectory(versionsDirectory())) {

            try (Stream<Path> children = Files.list(versionsDirectory())) {

                for (Path child : children.toList()) {

                    if (!keep.contains(child.getFileName().toString())) {
                        FileUtil.deleteRecursively(child);
                    }
                }
            }
        }

        FileUtil.deleteRecursively(stagingDirectory());
    }

    private Optional<String> readPointer(String name) {

        try {
            Path file = directory.resolve(name);

            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }

            String value = Files.readString(file, StandardCharsets.UTF_8).trim();

            return Versions.isValid(value) ? Optional.of(value) : Optional.empty();

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void writePointer(String name, String version) throws IOException {
        FileUtil.writeAtomic(directory.resolve(name), version);
    }

    private void deletePointer(String name) {

        try {
            Files.deleteIfExists(directory.resolve(name));
        } catch (IOException ignored) {
            // Un pointeur résiduel est sans conséquence : il est revalidé à chaque lecture.
        }
    }
}
