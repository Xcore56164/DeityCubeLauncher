package fr.deitycube.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class XstsAuthenticator {

    private static final String ENDPOINT =
            "https://xsts.auth.xboxlive.com/xsts/authorize";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private XstsAuthenticator() {
    }

    public static XstsToken authenticate(
            XboxLiveToken xboxLiveToken
    ) throws IOException, InterruptedException {

        String json = """
                {
                  "Properties": {
                    "SandboxId": "RETAIL",
                    "UserTokens": [
                      "%s"
                    ]
                  },
                  "RelyingParty": "rp://api.minecraftservices.com/",
                  "TokenType": "JWT"
                }
                """.formatted(
                xboxLiveToken.getToken()
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        json
                                )
                        )
                        .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println();
        System.out.println(
                "XSTS : HTTP "
                        + response.statusCode()
        );

        JsonNode root =
                OBJECT_MAPPER.readTree(
                        response.body()
                );

        if (response.statusCode() == 401) {

            String explanation = switch (root.path("XErr").asText("")) {
                case "2148916233" -> "Ce compte Microsoft n'a pas encore de profil Xbox. "
                        + "Connecte-toi une fois sur xbox.com pour le créer, puis réessaie.";
                case "2148916235" -> "Xbox Live n'est pas disponible dans le pays de ce compte Microsoft.";
                case "2148916236", "2148916237" -> "Ce compte Microsoft doit d'abord passer la vérification d'âge sur xbox.com.";
                case "2148916238" -> "Ce compte Microsoft appartient à un mineur : un adulte doit l'ajouter "
                        + "à une famille Microsoft (account.microsoft.com/family).";
                default -> null;
            };

            if (explanation != null) {
                throw new IOException(explanation);
            }
        }

        if (response.statusCode() != 200) {

            String xErr =
                    root.path("XErr").asText(
                            "unknown_error"
                    );

            String message =
                    root.path("Message").asText(
                            response.body()
                    );

            throw new IOException(
                    "Echec XSTS : "
                            + xErr
                            + " - "
                            + message
            );
        }

        String token =
                root.path("Token").asText(null);

        JsonNode userHashNode =
                root.path("DisplayClaims")
                        .path("xui")
                        .get(0);

        String userHash =
                userHashNode
                        .path("uhs")
                        .asText(null);

        if (token == null || token.isBlank()) {

            throw new IOException(
                    "XSTS n'a pas fourni de token."
            );
        }

        if (userHash == null || userHash.isBlank()) {

            throw new IOException(
                    "XSTS n'a pas fourni de userHash."
            );
        }

        return new XstsToken(
                token,
                userHash
        );
    }
}
