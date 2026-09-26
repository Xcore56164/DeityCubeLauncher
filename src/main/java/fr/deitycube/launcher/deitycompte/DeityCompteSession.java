package fr.deitycube.launcher.deitycompte;

import fr.deitycube.launcher.auth.AuthenticationMode;
import fr.deitycube.launcher.auth.AuthenticationResult;
import fr.deitycube.launcher.auth.MinecraftAuthenticator;
import fr.deitycube.launcher.auth.MinecraftProfile;
import fr.deitycube.launcher.auth.MinecraftProfileService;
import fr.deitycube.launcher.auth.MinecraftToken;
import fr.deitycube.launcher.auth.OfflineAuthenticator;
import fr.deitycube.launcher.auth.XboxLiveAuthenticator;
import fr.deitycube.launcher.auth.XboxLiveToken;
import fr.deitycube.launcher.auth.XstsAuthenticator;
import fr.deitycube.launcher.auth.XstsToken;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/** DeityCompte connecté au launcher, et accès au compte Minecraft officiel qui lui est lié. */
public final class DeityCompteSession {

    /**
     * Le jeton Minecraft (valable 24 h) est réutilisé tant qu'il lui reste au moins 1 h : une
     * partie peut durer, et rejoindre un serveur demande un jeton encore valide.
     */
    private static final long MINECRAFT_TOKEN_MARGIN_SECONDS = 3600;

    private final String sessionToken;
    private DeityCompteAccount account;

    private AuthenticationResult gameAuthentication;
    private Instant gameAuthenticationValidUntil = Instant.EPOCH;
    private String offlineFallbackReason;

    public DeityCompteSession(
            String sessionToken,
            DeityCompteAccount account
    ) {
        this.sessionToken = sessionToken;
        this.account = account;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public DeityCompteAccount getAccount() {
        return account;
    }

    public void setAccount(DeityCompteAccount account) {
        this.account = account;
    }

    /**
     * Profil et jeton avec lesquels lancer le jeu : compte Minecraft officiel si possible,
     * sinon mode hors ligne ("crack") sous le nom de jeu du DeityCompte, quand le site
     * l'autorise. Les problèmes du DeityCompte lui-même bloquent toujours le lancement.
     */
    public synchronized AuthenticationResult resolveGameAuthentication()
            throws IOException, InterruptedException {

        offlineFallbackReason = null;

        try {
            return resolveOfficialAuthentication();

        } catch (DeityCompteException e) {

            if (e.isAccountProblem() || !account.isOfflineFallbackAllowed()) {
                throw e;
            }
            return offlineFallback(e);

        } catch (IOException e) {

            if (!account.isOfflineFallbackAllowed()) {
                throw e;
            }
            return offlineFallback(e);
        }
    }

    /** Raison du dernier passage en mode hors ligne, ou null si le compte officiel a été utilisé. */
    public synchronized String getOfflineFallbackReason() {
        return offlineFallbackReason;
    }

    private AuthenticationResult offlineFallback(IOException cause) {

        offlineFallbackReason = cause.getMessage();

        System.out.println(
                "Connexion au compte Minecraft officiel impossible, passage en mode hors ligne : "
                        + offlineFallbackReason
        );

        return OfflineAuthenticator.authenticate(account.getOfflineName());
    }

    /**
     * Compte Minecraft officiel, gardé en mémoire (jamais sur le disque) tant que son jeton
     * reste valable. Sinon : jeton Microsoft demandé à deitycube.fr, puis Xbox Live → XSTS →
     * Minecraft Services depuis la connexion du joueur.
     */
    private AuthenticationResult resolveOfficialAuthentication()
            throws IOException, InterruptedException {

        if (gameAuthentication != null
                && Instant.now().isBefore(gameAuthenticationValidUntil)) {

            return gameAuthentication;
        }

        String microsoftAccessToken =
                DeityCompteApi.fetchMicrosoftAccessToken(sessionToken);

        XboxLiveToken xboxLiveToken =
                XboxLiveAuthenticator.authenticate(microsoftAccessToken);

        XstsToken xstsToken =
                XstsAuthenticator.authenticate(xboxLiveToken);

        MinecraftToken minecraftToken =
                MinecraftAuthenticator.authenticate(xstsToken);

        MinecraftProfile profile =
                MinecraftProfileService.fetchProfile(minecraftToken);

        gameAuthentication = new AuthenticationResult(
                AuthenticationMode.DEITYCOMPTE,
                profile.getName(),
                toUuid(profile.getId()),
                profile.getAccessToken()
        );

        gameAuthenticationValidUntil = Instant.now().plusSeconds(
                Math.max(0, minecraftToken.getExpiresIn() - MINECRAFT_TOKEN_MARGIN_SECONDS)
        );

        return gameAuthentication;
    }

    private static UUID toUuid(String id) throws IOException {

        if (id.length() != 32) {
            throw new IOException(
                    "Identifiant de profil Minecraft invalide : " + id
            );
        }

        return UUID.fromString(
                id.substring(0, 8) + "-"
                        + id.substring(8, 12) + "-"
                        + id.substring(12, 16) + "-"
                        + id.substring(16, 20) + "-"
                        + id.substring(20)
        );
    }
}
