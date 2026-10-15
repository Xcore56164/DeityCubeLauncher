import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Génère la paire de clés Ed25519 qui signe les payloads du launcher.
 *
 * Usage (JDK 21, aucune dépendance) :
 *   java tools/GenerateSigningKey.java [fichier-clé-privée]
 *
 * - La clé PRIVÉE est écrite dans un fichier : son contenu va dans le secret GitHub
 *   PAYLOAD_SIGNING_KEY (Settings > Secrets and variables > Actions). Ne la committez jamais, ne la
 *   copiez pas sur le VPS, gardez-en une sauvegarde hors ligne : la perdre oblige à réinstaller
 *   le launcher chez tous les joueurs.
 * - La clé PUBLIQUE est affichée : elle va dans PayloadSignature.EMBEDDED_PUBLIC_KEY, avant de
 *   construire l'installeur du bootstrap (elle y est figée).
 */
public final class GenerateSigningKey {

    public static void main(String[] args) throws Exception {

        Path privateKeyFile = Path.of(args.length > 0 ? args[0] : "deitycube-payload-signing.key");

        if (Files.exists(privateKeyFile)) {
            System.err.println("Refus d'écraser " + privateKeyFile + " (une clé existe déjà : supprimez-la d'abord si c'est voulu).");
            System.exit(1);
        }

        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

        Files.writeString(
                privateKeyFile,
                Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()) + System.lineSeparator(),
                StandardCharsets.UTF_8
        );

        try {
            Files.setPosixFilePermissions(privateKeyFile, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows : pas de permissions POSIX, le fichier reste dans votre profil.
        }

        System.out.println();
        System.out.println("Clé privée écrite dans : " + privateKeyFile.toAbsolutePath());
        System.out.println("  -> à coller dans le secret GitHub PAYLOAD_SIGNING_KEY, puis à mettre en lieu sûr");
        System.out.println();
        System.out.println("Clé publique (à coller dans PayloadSignature.EMBEDDED_PUBLIC_KEY) :");
        System.out.println();
        System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        System.out.println();
    }
}
