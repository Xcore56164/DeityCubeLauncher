package fr.deitycube.launcher.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class MicrosoftAuthenticator {

    private MicrosoftAuthenticator() {
    }

    public static MicrosoftAuthResult authenticate() throws Exception {

        String codeVerifier = PkceUtils.generateCodeVerifier();
        String codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier);
        String state = PkceUtils.generateState();

        CompletableFuture<String> authorizationCode =
                new CompletableFuture<>();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/", exchange -> {
            handleCallback(
                    exchange,
                    authorizationCode,
                    state
            );
        });

        server.start();

        int port = server.getAddress().getPort();

        String redirectUri =
                "http://localhost:" + port;

        System.out.println();
        System.out.println("==============================");
        System.out.println("     CONNEXION MICROSOFT");
        System.out.println("==============================");
        System.out.println();
        System.out.println("Serveur local : http://localhost:" + port);
        System.out.println();

        String authorizationUrl =
                MicrosoftAuthConfig.AUTHORIZATION_URL
                        + "?client_id=" + encode(MicrosoftAuthConfig.CLIENT_ID)
                        + "&response_type=code"
                        + "&redirect_uri=" + encode(
                        redirectUri
                )
                        + "&response_mode=query"
                        + "&scope=" + encode(
                        MicrosoftAuthConfig.SCOPE
                )
                        + "&code_challenge=" + encode(
                        codeChallenge
                )
                        + "&code_challenge_method=S256"
                        + "&state=" + encode(state);

        System.out.println("Ouverture de Microsoft...");

        if (!Desktop.isDesktopSupported()) {
            server.stop(0);
            throw new IllegalStateException(
                    "Le navigateur système n'est pas disponible."
            );
        }

        Desktop.getDesktop().browse(
                URI.create(authorizationUrl)
        );

        System.out.println(
                "Connecte-toi avec ton compte Microsoft."
        );

        String code;

        try {
            code = authorizationCode.get(
                    5,
                    TimeUnit.MINUTES
            );
        } finally {
            server.stop(0);
        }

        System.out.println();
        System.out.println(
                "Code d'autorisation reçu."
        );

        return new MicrosoftAuthResult(
                code,
                codeVerifier,
                redirectUri
        );
    }

    private static void handleCallback(
            HttpExchange exchange,
            CompletableFuture<String> authorizationCode,
            String expectedState
    ) throws IOException {

        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            sendResponse(
                    exchange,
                    "Erreur : aucun parametre recu."
            );

            authorizationCode.completeExceptionally(
                    new IllegalStateException(
                            "Aucun paramètre OAuth reçu."
                    )
            );

            return;
        }

        String code = getQueryParameter(
                query,
                "code"
        );

        String receivedState = getQueryParameter(
                query,
                "state"
        );

        String error = getQueryParameter(
                query,
                "error"
        );

        if (error != null) {

            String description = getQueryParameter(
                    query,
                    "error_description"
            );

            sendResponse(
                    exchange,
                    "Connexion Microsoft refusee. "
                            + "Tu peux fermer cette page."
            );

            authorizationCode.completeExceptionally(
                    new IllegalStateException(
                            "Erreur OAuth : "
                                    + error
                                    + " - "
                                    + description
                    )
            );

            return;
        }

        if (receivedState == null
                || !receivedState.equals(expectedState)) {

            sendResponse(
                    exchange,
                    "Erreur de securite OAuth. "
                            + "Tu peux fermer cette page."
            );

            authorizationCode.completeExceptionally(
                    new SecurityException(
                            "Le parametre OAuth state ne correspond pas."
                    )
            );

            return;
        }

        if (code == null || code.isBlank()) {

            sendResponse(
                    exchange,
                    "Erreur : aucun code d'autorisation."
            );

            authorizationCode.completeExceptionally(
                    new IllegalStateException(
                            "Code d'autorisation absent."
                    )
            );

            return;
        }

        sendResponse(
                exchange,
                """
                Connexion Microsoft reussie.
                
                Tu peux fermer cette fenetre et revenir au launcher.
                """
        );

        authorizationCode.complete(code);
    }

    private static String getQueryParameter(
            String query,
            String parameter
    ) {

        for (String pair : query.split("&")) {

            String[] parts = pair.split(
                    "=",
                    2
            );

            if (parts.length == 2
                    && parts[0].equals(parameter)) {

                return java.net.URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8
                );
            }
        }

        return null;
    }

    private static void sendResponse(
            HttpExchange exchange,
            String response
    ) throws IOException {

        byte[] bytes = response.getBytes(
                StandardCharsets.UTF_8
        );

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                200,
                bytes.length
        );

        try (OutputStream outputStream =
                     exchange.getResponseBody()) {

            outputStream.write(bytes);
        }
    }

    private static String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
