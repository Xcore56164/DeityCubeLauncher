package fr.deitycube.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class MinecraftProfileService {

    private static final String PROFILE_ENDPOINT =
            "https://api.minecraftservices.com/minecraft/profile";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private MinecraftProfileService() {
    }

    public static MinecraftProfile fetchProfile(
            MinecraftToken token
    ) throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(PROFILE_ENDPOINT))
                        .header(
                                "Authorization",
                                "Bearer " + token.getAccessToken()
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() == 404) {

            throw new IOException(
                    "Ce compte Microsoft ne possède pas Minecraft."
            );
        }

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
                    "Echec de récupération du profil Minecraft : "
                            + error
                            + " - "
                            + message
            );
        }

        String id =
                root.path("id").asText(null);

        String name =
                root.path("name").asText(null);

        if (id == null || id.isBlank()
                || name == null || name.isBlank()) {

            throw new IOException(
                    "Profil Minecraft incomplet."
            );
        }

        return new MinecraftProfile(
                id,
                name,
                token.getAccessToken()
        );
    }
}
