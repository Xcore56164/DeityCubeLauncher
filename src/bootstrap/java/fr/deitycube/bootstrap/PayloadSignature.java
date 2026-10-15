package fr.deitycube.bootstrap;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Signature Ed25519 des payloads. La clé privée reste dans les secrets de la CI : un serveur de
 * téléchargement compromis ne peut donc pas faire exécuter de code aux joueurs, ce qu'un simple
 * SHA-256 hébergé au même endroit que le fichier n'empêche pas.
 * <p>
 * CONTRAT FIGÉ : le message signé et la clé embarquée ne peuvent plus changer une fois le
 * bootstrap distribué (sauf réinstallation chez tous les joueurs).
 */
final class PayloadSignature {

    /**
     * Clé publique Ed25519 (encodage X.509, base64). À renseigner avec la sortie de
     * {@code java tools/GenerateSigningKey.java}. Tant qu'elle est vide, le bootstrap refuse
     * d'installer quoi que ce soit.
     */
    static final String EMBEDDED_PUBLIC_KEY = "MCowBQYDK2VwAyEAnbde4JJOZH8Iy5SDb3z6/Hm4KwoZD/PnK3WaVxgJKfg=";

    private static final String MESSAGE_PREFIX = "deitycube-payload-v1";

    private PayloadSignature() {
    }

    /** Lie la signature à la version et à l'OS : un vieux payload ne peut pas être relabellisé. */
    static byte[] message(String version, String platform, String sha256Hex) {

        return (MESSAGE_PREFIX + "\n" + version + "\n" + platform + "\n" + sha256Hex.toLowerCase())
                .getBytes(StandardCharsets.UTF_8);
    }

    static PublicKey embeddedKey() throws GeneralSecurityException {

        if (EMBEDDED_PUBLIC_KEY.isBlank()) {
            throw new GeneralSecurityException(
                    "Aucune clé publique de signature n'est configurée dans ce bootstrap."
            );
        }

        return parsePublicKey(EMBEDDED_PUBLIC_KEY);
    }

    static PublicKey parsePublicKey(String base64) throws GeneralSecurityException {

        return KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(Base64.getMimeDecoder().decode(base64.trim())));
    }

    static PrivateKey parsePrivateKey(String base64) throws GeneralSecurityException {

        return KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(base64.trim())));
    }

    static boolean verify(PublicKey key, byte[] message, String signatureBase64) {

        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(key);
            signature.update(message);

            return signature.verify(Base64.getMimeDecoder().decode(signatureBase64.trim()));

        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    static String sign(PrivateKey key, byte[] message) throws GeneralSecurityException {

        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(key);
        signature.update(message);

        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
