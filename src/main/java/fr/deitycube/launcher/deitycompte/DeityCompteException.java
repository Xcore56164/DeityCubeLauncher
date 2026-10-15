package fr.deitycube.launcher.deitycompte;

import java.io.IOException;

/**
 * Erreur renvoyée par l'API DeityCompte de deitycube.fr.
 * Le message est déjà rédigé pour le joueur ; l'URL (facultative) indique la page du site où
 * régler le problème (liaison Microsoft, acceptation des CGU...).
 */
public final class DeityCompteException extends IOException {

    /** Session inconnue ou expirée : il faut redemander l'email et le mot de passe. */
    public static final String SESSION_INVALIDE = "session_invalide";

    private final String code;
    private final String url;

    public DeityCompteException(String code, String message, String url) {
        this(code, message, url, null);
    }

    public DeityCompteException(String code, String message, String url, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    public boolean isSessionInvalid() {
        return SESSION_INVALIDE.equals(code);
    }

}
