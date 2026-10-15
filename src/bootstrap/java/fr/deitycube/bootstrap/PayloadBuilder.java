package fr.deitycube.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Outil de build (exclu du jar du bootstrap) : construit le zip d'un payload à partir du dossier
 * lib/ de installDist, écrit son SHA-256 et, si une clé privée est fournie, sa signature.
 * <p>
 * Il vit à côté du code qui lit les payloads pour que le format ne puisse pas diverger.
 * <pre>
 *   --lib-dir X --version 1.2.3 --platform windows --main-class a.b.Main --out fichier.zip
 *   [--jvm-arg -Xmx384m]... [--private-key-file F | variable PAYLOAD_SIGNING_KEY] [--require-signature]
 * </pre>
 */
public final class PayloadBuilder {

    /**
     * Date fixe des entrées (en heure locale, donc indépendante du fuseau horaire de la machine) :
     * deux builds identiques donnent un zip identique octet pour octet.
     */
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    record Result(Path zip, String sha256, String signature) {
    }

    private PayloadBuilder() {
    }

    public static void main(String[] args) throws Exception {

        Path libDir = null;
        Path out = null;
        String version = null;
        String platform = null;
        String mainClass = null;
        Path keyFile = null;
        boolean requireSignature = false;
        List<String> jvmArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {
                case "--lib-dir" -> libDir = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--version" -> version = args[++i];
                case "--platform" -> platform = args[++i];
                case "--main-class" -> mainClass = args[++i];
                case "--jvm-arg" -> jvmArgs.add(args[++i]);
                case "--private-key-file" -> keyFile = Path.of(args[++i]);
                case "--require-signature" -> requireSignature = true;
                default -> throw new IllegalArgumentException("Option inconnue : " + args[i]);
            }
        }

        if (libDir == null || out == null || version == null || platform == null || mainClass == null) {
            throw new IllegalArgumentException(
                    "Usage : --lib-dir X --out F --version V --platform P --main-class C [--jvm-arg A]..."
            );
        }

        String keyBase64 = keyFile != null
                ? Files.readString(keyFile, StandardCharsets.UTF_8)
                : System.getenv("PAYLOAD_SIGNING_KEY");

        PrivateKey key = keyBase64 != null && !keyBase64.isBlank()
                ? PayloadSignature.parsePrivateKey(keyBase64)
                : null;

        if (key == null && requireSignature) {
            throw new IllegalStateException(
                    "Signature exigée mais aucune clé privée fournie (PAYLOAD_SIGNING_KEY ou --private-key-file)."
            );
        }

        Result result = build(libDir, version, platform, mainClass, jvmArgs, out, key);

        System.out.println("Payload : " + result.zip());
        System.out.println("SHA-256 : " + result.sha256());
        System.out.println(result.signature() != null
                ? "Signature : écrite dans " + result.zip().getFileName() + ".sig"
                : "ATTENTION : payload NON signé (aucune clé privée fournie) — refusé par tout bootstrap.");
    }

    static Result build(
            Path libDir,
            String version,
            String platform,
            String mainClass,
            List<String> jvmArgs,
            Path outZip,
            PrivateKey key
    ) throws IOException, GeneralSecurityException {

        if (!Versions.isValid(version)) {
            throw new IllegalArgumentException("Version invalide : " + version);
        }

        List<Path> files;

        try (Stream<Path> stream = Files.list(libDir)) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        }

        if (files.isEmpty()) {
            throw new IOException("Aucun fichier dans " + libDir);
        }

        List<Map<String, Object>> entries = new ArrayList<>();

        for (Path file : files) {

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("path", "lib/" + file.getFileName());
            entry.put("size", Files.size(file));
            entry.put("sha256", FileUtil.sha256(file));
            entries.add(entry);
        }

        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("schema", 1);
        descriptor.put("version", version);
        descriptor.put("bootstrap_protocol", BootstrapMain.PROTOCOL);
        descriptor.put("main_class", mainClass);
        descriptor.put("jvm_args", jvmArgs);
        descriptor.put("files", entries);

        Files.createDirectories(outZip.toAbsolutePath().getParent());

        try (OutputStream fileOut = Files.newOutputStream(outZip);
             ZipOutputStream zip = new ZipOutputStream(fileOut)) {

            zip.setLevel(Deflater.BEST_COMPRESSION);

            put(zip, PayloadDescriptor.FILE_NAME, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(descriptor));

            for (Path file : files) {
                put(zip, "lib/" + file.getFileName(), Files.readAllBytes(file));
            }
        }

        String sha256 = FileUtil.sha256(outZip);

        Files.writeString(Path.of(outZip + ".sha256"), sha256 + System.lineSeparator(), StandardCharsets.UTF_8);

        String signature = null;

        if (key != null) {

            signature = PayloadSignature.sign(key, PayloadSignature.message(version, platform, sha256));

            Files.writeString(Path.of(outZip + ".sig"), signature + System.lineSeparator(), StandardCharsets.UTF_8);
        }

        return new Result(outZip, sha256, signature);
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {

        ZipEntry entry = new ZipEntry(name);
        entry.setTimeLocal(FIXED_TIME);

        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }
}
