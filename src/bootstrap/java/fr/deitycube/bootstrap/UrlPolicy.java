package fr.deitycube.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Seul HTTPS est accepté. L'exception du HTTP en boucle locale n'existe que pour les tests de
 * bout en bout (voir {@link BootstrapEnvironment}) : elle ne peut pas servir à joindre une
 * machine distante.
 */
final class UrlPolicy {

    private final boolean allowLoopbackHttp;

    private UrlPolicy(boolean allowLoopbackHttp) {
        this.allowLoopbackHttp = allowLoopbackHttp;
    }

    static UrlPolicy strict() {
        return new UrlPolicy(false);
    }

    static UrlPolicy allowLoopbackHttp() {
        return new UrlPolicy(true);
    }

    URI check(String url) throws IOException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException | NullPointerException e) {
            throw new IOException("URL invalide : " + url, e);
        }

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return uri;
        }

        if (allowLoopbackHttp
                && "http".equalsIgnoreCase(uri.getScheme())
                && isLoopback(uri.getHost())) {
            return uri;
        }

        throw new IOException("URL refusée (HTTPS requis) : " + url);
    }

    static boolean isLoopback(String host) {

        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "[::1]".equals(host);
    }
}
