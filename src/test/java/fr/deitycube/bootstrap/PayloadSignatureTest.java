package fr.deitycube.bootstrap;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadSignatureTest {

    private static final String SHA = "ab".repeat(32);

    @Test
    void acceptsAGenuineSignature() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        String signature = PayloadSignature.sign(keys.getPrivate(), PayloadSignature.message("1.2.3", "windows", SHA));

        assertTrue(PayloadSignature.verify(keys.getPublic(), PayloadSignature.message("1.2.3", "windows", SHA), signature));
    }

    @Test
    void rejectsAnyChangeToVersionPlatformOrHash() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        String signature = PayloadSignature.sign(keys.getPrivate(), PayloadSignature.message("1.2.3", "windows", SHA));

        // Un vieux payload ne doit pas pouvoir être présenté comme une version plus récente,
        // ni le payload d'un OS comme celui d'un autre.
        assertFalse(PayloadSignature.verify(keys.getPublic(), PayloadSignature.message("1.2.4", "windows", SHA), signature));
        assertFalse(PayloadSignature.verify(keys.getPublic(), PayloadSignature.message("1.2.3", "linux", SHA), signature));
        assertFalse(PayloadSignature.verify(keys.getPublic(), PayloadSignature.message("1.2.3", "windows", "cd".repeat(32)), signature));
    }

    @Test
    void rejectsASignatureFromAnotherKey() throws Exception {

        KeyPair genuine = TestSupport.newKeyPair();
        KeyPair attacker = TestSupport.newKeyPair();

        String forged = PayloadSignature.sign(attacker.getPrivate(), PayloadSignature.message("1.2.3", "windows", SHA));

        assertFalse(PayloadSignature.verify(genuine.getPublic(), PayloadSignature.message("1.2.3", "windows", SHA), forged));
    }

    @Test
    void rejectsGarbageWithoutThrowing() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        byte[] message = PayloadSignature.message("1.2.3", "windows", SHA);

        assertFalse(PayloadSignature.verify(keys.getPublic(), message, ""));
        assertFalse(PayloadSignature.verify(keys.getPublic(), message, "%%% pas du base64 %%%"));
        assertFalse(PayloadSignature.verify(keys.getPublic(), message, Base64.getEncoder().encodeToString(new byte[64])));
    }

    @Test
    void keysSurviveTheirBase64Encoding() throws Exception {

        KeyPair keys = TestSupport.newKeyPair();

        String publicB64 = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
        String privateB64 = Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());

        byte[] message = PayloadSignature.message("1.0.0", "linux", SHA);

        String signature = PayloadSignature.sign(PayloadSignature.parsePrivateKey(privateB64), message);

        assertTrue(PayloadSignature.verify(PayloadSignature.parsePublicKey(publicB64), message, signature));
    }

    @Test
    void theEmbeddedKeyIsEitherAbsentAndRefusedOrAValidEd25519Key() throws Exception {

        if (PayloadSignature.EMBEDDED_PUBLIC_KEY.isBlank()) {
            // Tant que tools/GenerateSigningKey.java n'a pas servi, rien ne doit s'installer.
            assertThrows(Exception.class, PayloadSignature::embeddedKey);
            return;
        }

        // Une clé collée de travers (tronquée, caractère en trop) ferait refuser toutes les mises
        // à jour par un bootstrap qu'on ne peut plus corriger : on le détecte ici, pas chez les joueurs.
        EdECPublicKey key = (EdECPublicKey) PayloadSignature.embeddedKey();

        assertEquals("Ed25519", key.getParams().getName());
    }
}
