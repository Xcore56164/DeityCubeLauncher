package fr.deitycube.launcher.deitycompte;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.deitycube.launcher.config.AppVersion;
import fr.deitycube.launcher.config.LauncherConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client de l'API launcher de deitycube.fr (voir includes/launcher.php côté site).
 *
 * Le launcher échange une seule fois l'email et le mot de passe du DeityCompte contre un jeton
 * de session, puis demande à chaque lancement un jeton d'accès Microsoft temporaire : le jeton
 * de rafraîchissement Microsoft reste sur le serveur. Le launcher enchaîne ensuite lui-même
 * Xbox Live → XSTS → Minecraft Services, depuis l'adresse IP du joueur.
 */
public final class DeityCompteApi {

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofSeconds(20);

    private DeityCompteApi() {
    }

    public static DeityCompteSession login(
            String email,
            String password
    ) throws IOException, InterruptedException {

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("email", email);
        body.put("mot_de_passe", password);
        body.put("appareil", deviceName());

        JsonNode json = send(
                HttpRequest.newBuilder(endpoint("connexion.php"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                OBJECT_MAPPER.writeValueAsString(body)
                        )),
                DEFAULT_TIMEOUT
        );

        String sessionToken = json.path("session").asText("");

        if (sessionToken.isBlank()) {
            throw new DeityCompteException(
                    "reponse_invalide",
                    "Réponse inattendue de deitycube.fr.",
                    null
            );
        }

        return new DeityCompteSession(
                sessionToken,
                DeityCompteAccount.fromJson(json.path("compte"))
        );
    }

    public static DeityCompteAccount fetchAccount(
            String sessionToken
    ) throws IOException, InterruptedException {

        JsonNode json = send(
                authorized(endpoint("compte.php"), sessionToken).GET(),
                DEFAULT_TIMEOUT
        );

        return DeityCompteAccount.fromJson(json.path("compte"));
    }

    /** Jeton d'accès Microsoft (portée XboxLive.signin, valable ~1 h) du compte lié. */
    public static String fetchMicrosoftAccessToken(
            String sessionToken
    ) throws IOException, InterruptedException {

        JsonNode json = send(
                authorized(endpoint("microsoft.php"), sessionToken)
                        .POST(HttpRequest.BodyPublishers.noBody()),
                DEFAULT_TIMEOUT
        );

        String accessToken = json.path("access_token").asText("");

        if (accessToken.isBlank()) {
            throw new DeityCompteException(
                    "reponse_invalide",
                    "deitycube.fr n'a pas fourni d'accès Microsoft.",
                    null
            );
        }

        return accessToken;
    }

    /**
     * Manifeste du modpack du serveur test (format de deitycube_manifest.json, plus l'adresse
     * du serveur dans "server"), réservé aux rôles qui ont accès au serveur test.
     */
    public static JsonNode fetchTestManifest(
            String sessionToken
    ) throws IOException, InterruptedException {

        try {
            return send(
                    authorized(endpoint("test.php"), sessionToken).GET(),
                    DEFAULT_TIMEOUT
            );
        } catch (DeityCompteException e) {

            String message = testManifestErrorMessage(e.getCode());

            if (message == null) {
                throw e;
            }

            throw new DeityCompteException(e.getCode(), message, null, e);
        }
    }

    /** Message pour le joueur selon le code d'erreur de test.php (null : message du site). */
    static String testManifestErrorMessage(String code) {

        return switch (code) {
            case DeityCompteException.ACCES_TEST_REFUSE ->
                    "Le serveur test est réservé à l'équipe.";
            case DeityCompteException.SERVEUR_TEST_VIDE ->
                    "Aucun mod n'est en test pour le moment.";
            default -> null;
        };
    }

    /** Ferme la session côté site (elle disparaît de la liste des appareils du compte). */
    public static void logout(
            String sessionToken,
            Duration timeout
    ) throws IOException, InterruptedException {

        send(
                authorized(endpoint("deconnexion.php"), sessionToken)
                        .POST(HttpRequest.BodyPublishers.noBody()),
                timeout
        );
    }

    private static HttpRequest.Builder authorized(
            URI uri,
            String sessionToken
    ) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + sessionToken);
    }

    private static JsonNode send(
            HttpRequest.Builder builder,
            Duration timeout
    ) throws IOException, InterruptedException {

        HttpRequest request = builder
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "DeityCubeLauncher/" + AppVersion.CURRENT)
                .build();

        HttpResponse<String> response;

        try {
            response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException e) {
            throw new DeityCompteException(
                    "site_injoignable",
                    "Impossible de joindre deitycube.fr. Vérifie ta connexion internet puis réessaie.",
                    null,
                    e
            );
        }

        JsonNode json;

        try {
            json = OBJECT_MAPPER.readTree(response.body());
        } catch (JsonProcessingException e) {
            json = null;
        }

        if (response.statusCode() == 200 && json != null && json.isObject()) {
            return json;
        }

        if (json == null || !json.isObject()) {
            throw new DeityCompteException(
                    "reponse_invalide",
                    "Réponse inattendue de deitycube.fr (HTTP " + response.statusCode() + ").",
                    null
            );
        }

        throw new DeityCompteException(
                json.path("erreur").asText("erreur_inconnue"),
                json.path("message").asText("Erreur inattendue de deitycube.fr."),
                json.hasNonNull("url") ? json.get("url").asText() : null
        );
    }

    private static URI endpoint(String file) {
        return URI.create(LauncherConfig.DEITYCOMPTE_API_URL + file);
    }

    /** Nom affiché dans la liste des appareils du compte sur le site. */
    private static String deviceName() {

        String host = System.getenv("COMPUTERNAME");

        if (host == null || host.isBlank()) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (IOException e) {
                host = null;
            }
        }

        String os = System.getProperty("os.name", "");

        String name = host != null && !host.isBlank()
                ? host + " (" + os + ")"
                : "Launcher DeityCube (" + os + ")";

        return name.length() > 64 ? name.substring(0, 64) : name;
    }
}
