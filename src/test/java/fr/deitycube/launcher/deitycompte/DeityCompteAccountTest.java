package fr.deitycube.launcher.deitycompte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeityCompteAccountTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static DeityCompteAccount account(String json) throws Exception {
        return DeityCompteAccount.fromJson(OBJECT_MAPPER.readTree(json));
    }

    @Test
    void readsTestAccess() throws Exception {

        assertTrue(account("{\"pseudo\":\"Xcore\",\"acces_test\":true}").hasTestAccess());
        assertFalse(account("{\"pseudo\":\"Xcore\",\"acces_test\":false}").hasTestAccess());
    }

    @Test
    void testAccessDefaultsToFalse() throws Exception {

        // Site pas encore à jour (pas de champ), ou valeur nulle.
        assertFalse(account("{\"pseudo\":\"Xcore\",\"role\":\"joueur\"}").hasTestAccess());
        assertFalse(account("{\"pseudo\":\"Xcore\",\"acces_test\":null}").hasTestAccess());
    }

    @Test
    void explainsTestServerRefusals() {

        assertEquals(
                "Le serveur test est réservé à l'équipe.",
                DeityCompteApi.testManifestErrorMessage(DeityCompteException.ACCES_TEST_REFUSE)
        );

        assertEquals(
                "Aucun mod n'est en test pour le moment.",
                DeityCompteApi.testManifestErrorMessage(DeityCompteException.SERVEUR_TEST_VIDE)
        );

        // Les autres erreurs (session expirée...) gardent le message du site.
        assertNull(DeityCompteApi.testManifestErrorMessage(DeityCompteException.SESSION_INVALIDE));
    }
}
