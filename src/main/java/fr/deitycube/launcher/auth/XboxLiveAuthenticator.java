package fr.deitycube.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class XboxLiveAuthenticator {

    private static final String ENDPOINT =
            "https://user.auth.xboxlive.com/user/authenticate";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private XboxLiveAuthenticator() {
    }

    public static XboxLiveToken authenticate(
            String microsoftAccessToken
    ) throws IOException, InterruptedException {

        String json = """
                {
                  "Properties": {
                    "AuthMethod": "RPS",
                    "SiteName": "user.auth.xboxlive.com",
                    "RpsTicket": "d=%s"
                  },
                  "RelyingParty": "http://auth.xboxlive.com",
                  "TokenType": "JWT"
                }
                """.formatted(
                microsoftAccessToken
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .header(
                                "Content-Type",
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
                "Xbox Live : HTTP "
                        + response.statusCode()
        );

        JsonNode root =
                OBJECT_MAPPER.readTree(
                        response.body()
                );

        if (response.statusCode() != 200) {

            String error =
                    root.path("XErr").asText(
                            "unknown_error"
                    );

            String message =
                    root.path("Message").asText(
                            response.body()
                    );

            throw new IOException(
                    "Echec Xbox Live : "
                            + error
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
                    "Xbox Live n'a pas fourni de token."
            );
        }

        if (userHash == null || userHash.isBlank()) {

            throw new IOException(
                    "Xbox Live n'a pas fourni de userHash."
            );
        }

        return new XboxLiveToken(
                token,
                userHash
        );
    }
}
