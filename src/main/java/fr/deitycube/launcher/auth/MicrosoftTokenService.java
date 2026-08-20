package fr.deitycube.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MicrosoftTokenService {

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private MicrosoftTokenService() {
    }

    public static MicrosoftTokenResponse exchangeCode(
            MicrosoftAuthResult authResult
    ) throws IOException, InterruptedException {

        String body =
                "client_id=" + encode(
                        MicrosoftAuthConfig.CLIENT_ID
                )
                        + "&grant_type=authorization_code"
                        + "&code=" + encode(
                        authResult.getAuthorizationCode()
                )
                        + "&redirect_uri=" + encode(
                        authResult.getRedirectUri()
                )
                        + "&code_verifier=" + encode(
                        authResult.getCodeVerifier()
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                MicrosoftAuthConfig.TOKEN_URL
                        ))
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        body
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
                "Reponse Microsoft : HTTP "
                        + response.statusCode()
        );

        JsonNode json =
                OBJECT_MAPPER.readTree(
                        response.body()
                );

        if (response.statusCode() != 200) {

            String error =
                    json.path("error").asText(
                            "unknown_error"
                    );

            String description =
                    json.path("error_description").asText(
                            "Aucune description."
                    );

            throw new IOException(
                    "Echec de l'echange du token Microsoft : "
                            + error
                            + " - "
                            + description
            );
        }

        String accessToken =
                json.path("access_token").asText(null);

        String refreshToken =
                json.path("refresh_token").asText(null);

        String tokenType =
                json.path("token_type").asText(null);

        String scope =
                json.path("scope").asText(null);

        long expiresIn =
                json.path("expires_in").asLong(0);

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IOException(
                    "Microsoft n'a pas fourni d'access_token."
            );
        }

        return new MicrosoftTokenResponse(
                accessToken,
                refreshToken,
                tokenType,
                scope,
                expiresIn
        );
    }

    private static String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
