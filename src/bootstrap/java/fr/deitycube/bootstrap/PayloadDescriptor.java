package fr.deitycube.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contenu de {@code payload.json}, à la racine de chaque payload. CONTRAT FIGÉ : le schéma ne
 * peut qu'être étendu (champs ajoutés), jamais modifié, tant que des bootstraps l'utilisent.
 */
record PayloadDescriptor(
        String version,
        int protocol,
        String mainClass,
        List<String> jvmArgs,
        List<FileEntry> files
) {

    static final String FILE_NAME = "payload.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    record FileEntry(String path, long size, String sha256) {
    }

    static PayloadDescriptor read(Path directory) throws IOException {
        return parse(Files.readAllBytes(directory.resolve(FILE_NAME)));
    }

    static PayloadDescriptor parse(byte[] json) throws IOException {

        JsonNode root = MAPPER.readTree(json);

        String version = root.path("version").asText(null);

        if (!Versions.isValid(version)) {
            throw new IOException("payload.json : version invalide");
        }

        String mainClass = root.path("main_class").asText("");

        if (mainClass.isBlank()) {
            throw new IOException("payload.json : main_class absent");
        }

        List<String> jvmArgs = new ArrayList<>();

        for (JsonNode arg : root.path("jvm_args")) {
            jvmArgs.add(arg.asText());
        }

        List<FileEntry> files = new ArrayList<>();

        for (JsonNode file : root.path("files")) {

            String path = file.path("path").asText("");
            String sha256 = file.path("sha256").asText("");

            if (!isSafeRelativePath(path) || !SHA256.matcher(sha256).matches()) {
                throw new IOException("payload.json : entrée de fichier invalide : " + path);
            }

            files.add(new FileEntry(path, file.path("size").asLong(-1), sha256.toLowerCase()));
        }

        if (files.isEmpty()) {
            throw new IOException("payload.json : aucun fichier listé");
        }

        return new PayloadDescriptor(
                version,
                root.path("bootstrap_protocol").asInt(1),
                mainClass,
                List.copyOf(jvmArgs),
                List.copyOf(files)
        );
    }

    /** Contrôle rapide (présence et taille), sans lire le contenu : fait à chaque démarrage. */
    List<String> missingFiles(Path directory) {

        List<String> problems = new ArrayList<>();

        for (FileEntry entry : files) {

            Path file = directory.resolve(entry.path());

            try {
                if (!Files.isRegularFile(file)) {
                    problems.add(entry.path() + " (absent)");
                } else if (entry.size() >= 0 && Files.size(file) != entry.size()) {
                    problems.add(entry.path() + " (taille inattendue)");
                }
            } catch (IOException e) {
                problems.add(entry.path() + " (illisible)");
            }
        }

        return problems;
    }

    /** Contrôle complet (empreinte de chaque fichier) : fait après l'extraction d'un payload. */
    List<String> corruptedFiles(Path directory) {

        List<String> problems = new ArrayList<>(missingFiles(directory));

        if (!problems.isEmpty()) {
            return problems;
        }

        for (FileEntry entry : files) {

            try {
                if (!FileUtil.sha256(directory.resolve(entry.path())).equalsIgnoreCase(entry.sha256())) {
                    problems.add(entry.path() + " (contenu modifié)");
                }
            } catch (IOException e) {
                problems.add(entry.path() + " (illisible)");
            }
        }

        return problems;
    }

    static boolean isSafeRelativePath(String path) {

        if (path == null || path.isBlank()
                || path.startsWith("/")
                || path.contains("\\")
                || path.contains(":")) {
            return false;
        }

        for (String segment : path.split("/")) {

            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }

        return true;
    }
}
