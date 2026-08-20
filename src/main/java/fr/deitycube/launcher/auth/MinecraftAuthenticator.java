package fr.deitycube.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MinecraftAuthenticator {
    private static final String LOGIN_ENDPOINT =
            "https://api.minecraftservices.com/authentication/login_with_xbox";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private MinecraftAuthenticator() {
    }

    public static MinecraftToken authenticate(
            XstsToken xstsToken
    ) throws IOException, InterruptedException {

        String identityToken =
                "XBL3.0 x="
                        + xstsToken.getUserHash()
                        + ";"
                        + xstsToken.getToken();

        String json = """
                {
                    "identityToken": "%s"
                }
                """.formatted(identityToken);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(LOGIN_ENDPOINT))
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
                "Minecraft Services : HTTP"
                        + response.statusCode()
        );

        JsonNode root =
                OBJECT_MAPPER.readTree(
                        response.body()
                );

        if (response.statusCode() != 200) {
            String error =
                    root.path("error").asText(
                            "unknown_error"
                    );

            String message =
                    root.path("errorMessage").asText(
                            response.body()
                    );

            throw new IOException(
                    "Echec Minecraft Services"
                            + error
                            + " - "
                            + message
            );
        }

        String accessToken =
                root.path("access_token").asText(null);

        String tokenType =
                root.path("token_type").asText(null);

        long expiresIn =
                root.path("expires_in").asLong(0);

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IOException(
                    "Minecraft Services n'a pas fourni "
                            + "d'access_token."
            );
        }

        return new MinecraftToken(
                accessToken,
                tokenType,
                expiresIn
        );
    }
}
