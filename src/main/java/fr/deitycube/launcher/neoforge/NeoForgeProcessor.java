package fr.deitycube.launcher.neoforge;

import com.fasterxml.jackson.databind.JsonNode;
import fr.deitycube.launcher.network.HttpDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class NeoForgeProcessor {

    private final Path gameDirectory;
    private final Path minecraftJar;
    private final Path installer;
    private final NeoForgeInstallerProfile profile;
    private final NeoForgePathResolver pathResolver;

    public NeoForgeProcessor(
            Path gameDirectory,
            Path minecraftJar,
            Path installer,
            NeoForgeInstallerProfile profile,
            NeoForgePathResolver pathResolver
    ) {
        this.gameDirectory = gameDirectory;
        this.minecraftJar = minecraftJar;
        this.installer = installer;
        this.profile = profile;
        this.pathResolver = pathResolver;
    }

    private static final String NEOFORGE_MAVEN =
            "https://maven.neoforged.net/releases/";

    private String buildMavenUrl(
            MavenArtifact artifact
    ) {

        return NEOFORGE_MAVEN
                + artifact.getRelativePath();
    }

    private Path getLibraryPath(
            MavenArtifact artifact
    ) {

        return gameDirectory
                .resolve("libraries")
                .resolve(
                        artifact.getRelativePath()
                );
    }

    public void prepare()
            throws IOException, InterruptedException{

        List<NeoForgeLibrary> libraries =
                readLibraries(profile);

        for (NeoForgeLibrary library : libraries) {
            downloadLibrary(library);
        }

        pathResolver.register(
                "INSTALLER",
                installer
        );

        registerDataTokens();

        List<NeoForgeProcessorDefinition> processors =
                readClientProcessors();

        downloadProcessorDependencies(
                processors
        );

        System.out.println();
        System.out.println(
                "Processors client : "
                        + processors.size()
        );

        for (NeoForgeProcessorDefinition processor :
                processors) {

            System.out.println();
            System.out.println(
                    "Processor : "
                            + processor.getJar()
            );

            System.out.println(
                    "Classpath : "
                            + processor.getClasspath().size()
                            + " éléments"
            );

            System.out.println(
                    "Arguments : "
                            + processor.getArgs().size()
                            + " éléments"
            );

        }

        Path javaExecutable =
                Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        System.getProperty("os.name")
                                .toLowerCase()
                                .contains("win")
                                ? "java.exe"
                                : "java"
                );

        NeoForgeProcessorExecutor executor =
                new NeoForgeProcessorExecutor(
                        javaExecutable,
                        gameDirectory,
                        pathResolver
                );

        for (NeoForgeProcessorDefinition processor :
                processors) {

            executor.execute(processor);
        }

        verifyClientArtifacts();
    }

    private void verifyClientArtifacts() throws IOException {

        for (Path artifact : List.of(
                getPatchedClientPath(),
                getSrgClientPath(),
                getExtraClientPath()
        )) {

            if (!Files.isRegularFile(artifact)) {

                throw new IOException(
                        "Artefact client NeoForge introuvable : "
                                + artifact
                );
            }
        }
    }

    private Path getPatchedClientPath() {

        return pathResolver.resolvePlaceholder(
                "{PATCHED}"
        );
    }

    private Path getExtraClientPath() {

        return pathResolver.resolvePlaceholder(
                "{MC_EXTRA}"
        );
    }

    private Path getSrgClientPath() {

        return pathResolver.resolvePlaceholder(
                "{MC_SRG}"
        );
    }

    private void registerDataTokens() throws IOException {

        JsonNode dataNode =
                profile.getInstallProfile()
                        .path("data");

        if (!dataNode.isObject()) {

            throw new IllegalStateException(
                    "Section 'data' absente ou invalide "
                            + "dans install_profile.json."
            );
        }

        var fields =
                dataNode.fields();

        while (fields.hasNext()) {

            var field =
                    fields.next();

            String value =
                    field.getValue()
                            .path("client")
                            .asText(null);

            if (value == null || value.isBlank()) {
                continue;
            }

            registerDataToken(
                    field.getKey(),
                    value
            );
        }
    }

    private void registerDataToken(
            String token,
            String value
    ) throws IOException {

        if (value.startsWith("[")
                && value.endsWith("]")) {

            Path resolved =
                    pathResolver.resolveMaven(
                            value
                    );

            Files.createDirectories(
                    resolved.getParent()
            );

            pathResolver.register(
                    token,
                    resolved
            );

            return;
        }

        if (value.startsWith("/")) {

            pathResolver.register(
                    token,
                    extractInstallerResource(
                            value
                    )
            );

            return;
        }

        String literal = value;

        if (literal.length() >= 2
                && literal.startsWith("'")
                && literal.endsWith("'")) {

            literal = literal.substring(
                    1,
                    literal.length() - 1
            );
        }

        pathResolver.registerVariable(
                token,
                literal
        );
    }

    private Path extractInstallerResource(
            String entryPath
    ) throws IOException {

        String relative =
                entryPath.startsWith("/")
                        ? entryPath.substring(1)
                        : entryPath;

        String fileName =
                relative.substring(
                        relative.lastIndexOf('/') + 1
                );

        Path destination =
                gameDirectory
                        .resolve("neoforge")
                        .resolve(fileName);

        Files.createDirectories(
                destination.getParent()
        );

        try (JarFile jarFile =
                     new JarFile(installer.toFile())) {

            var entry =
                    jarFile.getJarEntry(
                            relative
                    );

            if (entry == null) {

                throw new IOException(
                        "Ressource absente de l'installer NeoForge : "
                                + relative
                );
            }

            try (InputStream input =
                         jarFile.getInputStream(entry)) {

                Files.copy(
                        input,
                        destination,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        }

        return destination;
    }

    private List<NeoForgeLibrary> readLibraries(
            NeoForgeInstallerProfile profile
    ) {

        List<NeoForgeLibrary> libraries =
                new ArrayList<>();

        JsonNode librariesNode =
                profile
                        .getInstallProfile()
                        .path("libraries");

        if (!librariesNode.isArray()) {

            throw new IllegalStateException(
                    "Le profil NeoForge ne contient pas "
                            + "de tableau libraries valide."
            );
        }

        for (JsonNode libraryNode : librariesNode) {

            String name =
                    libraryNode
                            .path("name")
                            .asText(null);

            if (name == null || name.isBlank()) {

                throw new IllegalStateException(
                        "Une bibliothèque NeoForge "
                                + "ne possède pas de nom."
                );
            }

            MavenArtifact artifact =
                    MavenArtifact.parse(name);

            JsonNode artifactNode =
                    libraryNode
                            .path("downloads")
                            .path("artifact");

            String url =
                    artifactNode
                            .path("url")
                            .asText(null);

            String sha1 =
                    artifactNode
                            .path("sha1")
                            .asText(null);

            if (url == null || url.isBlank()) {

                throw new IllegalStateException(
                        "URL absente pour la bibliothèque NeoForge : "
                                + name
                );
            }

            if (sha1 == null || sha1.isBlank()) {

                throw new IllegalStateException(
                        "SHA-1 absent pour la bibliothèque NeoForge : "
                                + name
                );
            }

            libraries.add(
                    new NeoForgeLibrary(
                            artifact,
                            url,
                            sha1
                    )
            );
        }

        return libraries;
    }

    private void downloadLibrary(
            NeoForgeLibrary library
    ) throws IOException {

        MavenArtifact artifact =
                library.getArtifact();

        String url =
                library.getUrl();

        String expectedSha1 =
                library.getSha1();

        Path destination =
                gameDirectory
                        .resolve("libraries")
                        .resolve(
                                artifact.getRelativePath()
                        );

        if (Files.exists(destination)) {

            System.out.println(
                    "Vérification NeoForge : "
                            + library.getCoordinate()
            );

            if (HttpDownloader.verifySha1(
                    destination,
                    expectedSha1
            )) {

                System.out.println(
                        "Fichier déjà présent et valide : "
                                + destination.getFileName()
                );

                return;
            }

            System.out.println(
                    "SHA-1 invalide, nouveau téléchargement : "
                            + destination.getFileName()
            );
        } else {

            System.out.println(
                    "Téléchargement NeoForge : "
                            + library.getCoordinate()
            );
        }

        HttpDownloader.downloadSha1(
                url,
                destination,
                expectedSha1
        );
    }

    private void downloadProcessorDependency(
            String coordinate
    ) throws IOException {

        MavenArtifact artifact =
                MavenArtifact.parse(coordinate);

        String url =
                buildMavenUrl(artifact);

        Path destination =
                getLibraryPath(artifact);

        System.out.println();
        System.out.println(
                "Téléchargement dépendance Processor : "
                        + coordinate
        );

        System.out.println(
                "URL : "
                        + url
        );

        if (Files.exists(destination)) {

            System.out.println(
                    "Vérification SHA-256 : "
                            + destination.getFileName()
            );

            String expectedSha256 =
                    HttpDownloader.downloadText(
                            url + ".sha256"
                    );

            if (expectedSha256.contains(" ")) {
                expectedSha256 =
                        expectedSha256.substring(
                                0,
                                expectedSha256.indexOf(' ')
                        );
            }

            if (HttpDownloader.verifySha256(
                    destination,
                    expectedSha256
            )) {

                System.out.println(
                        "Fichier déjà présent et valide : "
                                + destination.getFileName()
                );

                return;
            }

            System.out.println(
                    "SHA-256 invalide, nouveau téléchargement : "
                            + destination.getFileName()
            );
        }

        String expectedSha256 =
                HttpDownloader.downloadText(
                        url + ".sha256"
                );

        if (expectedSha256.contains(" ")) {

            expectedSha256 =
                    expectedSha256.substring(
                            0,
                            expectedSha256.indexOf(' ')
                    );
        }

        if (expectedSha256.isBlank()) {

            throw new IOException(
                    "SHA-256 vide pour : "
                            + coordinate
            );
        }

        System.out.println(
                "SHA-256 : vérification..."
        );

        HttpDownloader.downloadSha256(
                url,
                destination,
                expectedSha256
        );
    }

    private void downloadProcessorDependencies(
            List<NeoForgeProcessorDefinition> processors
    ) throws IOException {

        for (NeoForgeProcessorDefinition processor :
                processors) {

            for (String argument :
                    processor.getArgs()) {

                if (!argument.startsWith("[")
                        || !argument.endsWith("]")) {
                    continue;
                }

                String coordinate =
                        argument.substring(
                                1,
                                argument.length() - 1
                        );

                downloadProcessorDependency(
                        coordinate
                );
            }
        }
    }

    private String readMainClass(Path jar) throws IOException {

        try (JarFile jarFile = new JarFile(jar.toFile())) {

            Manifest manifest =
                    jarFile.getManifest();

            if (manifest == null) {
                throw new IOException(
                        "Manifest absent du processor : "
                                + jar
                );
            }

            String mainClass =
                    manifest
                            .getMainAttributes()
                            .getValue("Main-Class");

            if (mainClass == null
                    || mainClass.isBlank()) {

                throw new IOException(
                        "Main-Class absent du processor : "
                                + jar
                );
            }

            return mainClass;
        }
    }

    private List<NeoForgeProcessorDefinition> readClientProcessors() throws IOException {

        List<NeoForgeProcessorDefinition> processors =
                new ArrayList<>();

        JsonNode processorsNode =
                profile
                        .getInstallProfile()
                        .path("processors");

        for (JsonNode processorNode : processorsNode) {

            JsonNode sidesNode = processorNode.get("sides");

            boolean client;

            if (sidesNode == null || !sidesNode.isArray() || sidesNode.isEmpty()) {
                client = true;
            } else {
                client = false;

                for (JsonNode side : sidesNode) {
                    if ("client".equalsIgnoreCase(side.asText())) {
                        client = true;
                        break;
                    }
                }
            }

            if (!client) {
                continue;
            }

            String jar =
                    processorNode
                            .path("jar")
                            .asText(null);

            if (jar == null || jar.isBlank()) {
                throw new IllegalStateException(
                        "Processor client sans JAR."
                );
            }

            List<String> classpath =
                    new ArrayList<>();

            for (JsonNode entry :
                    processorNode.path("classpath")) {

                classpath.add(
                        entry.asText()
                );
            }

            List<String> args =
                    new ArrayList<>();

            for (JsonNode entry :
                    processorNode.path("args")) {

                args.add(
                        entry.asText()
                );
            }

            Path processorJar =
                    pathResolver.resolveMaven(jar);

            String mainClass =
                    readMainClass(processorJar);

            processors.add(
                    new NeoForgeProcessorDefinition(
                            jar,
                            classpath,
                            args,
                            mainClass
                    )
            );
        }

        return processors;
    }
}
