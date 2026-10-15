package fr.deitycube.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

final class ManifestClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    private final HttpFetcher http;
    private final UrlPolicy policy;
    private final Os os;

    ManifestClient(HttpFetcher http, UrlPolicy policy, Os os) {
        this.http = http;
        this.policy = policy;
        this.os = os;
    }

    UpdateManifest fetch(String manifestUrl) throws IOException {
        return parse(http.getText(policy.check(manifestUrl)));
    }

    UpdateManifest parse(String json) throws IOException {

        JsonNode root = MAPPER.readTree(json);

        String version = root.path("version").asText(null);

        if (!Versions.isValid(version)) {
            throw new IOException("Manifeste : version invalide");
        }

        UpdateManifest.PayloadTarget target = null;

        String key = os.payloadKey();

        if (key != null) {

            JsonNode node = root.path("payload").path(key);

            if (node.isObject()) {

                String url = node.path("url").asText("");
                String sha256 = node.path("sha256").asText("");
                String signature = node.path("signature").asText("");

                if (url.isBlank() || !SHA256.matcher(sha256).matches() || !isBase64(signature)) {
                    throw new IOException("Manifeste : bloc payload invalide pour " + key);
                }

                target = new UpdateManifest.PayloadTarget(url, sha256.toLowerCase(), signature);
            }
        }

        return new UpdateManifest(
                version,
                root.path("notes").asText(""),
                root.path("bootstrap_protocol").asInt(1),
                target
        );
    }

    private static boolean isBase64(String value) {

        try {
            return !value.isBlank() && Base64.getMimeDecoder().decode(value).length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
