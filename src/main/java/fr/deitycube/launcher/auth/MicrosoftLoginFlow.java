package fr.deitycube.launcher.auth;

import java.io.IOException;
import java.util.UUID;

public final class MicrosoftLoginFlow {

    private MicrosoftLoginFlow() {
    }

    public static MicrosoftLoginResult login() throws Exception {

        MicrosoftAuthResult authResult =
                MicrosoftAuthenticator.authenticate();

        MicrosoftTokenResponse tokenResponse =
                MicrosoftTokenService.exchangeCode(authResult);

        return finish(tokenResponse);
    }

    public static MicrosoftLoginResult loginWithRefreshToken(
            String refreshToken
    ) throws Exception {

        MicrosoftTokenResponse tokenResponse =
                MicrosoftTokenService.refreshToken(refreshToken);

        return finish(tokenResponse);
    }

    private static MicrosoftLoginResult finish(
            MicrosoftTokenResponse tokenResponse
    ) throws Exception {

        XboxLiveToken xboxLiveToken =
                XboxLiveAuthenticator.authenticate(
                        tokenResponse.getAccessToken()
                );

        XstsToken xstsToken =
                XstsAuthenticator.authenticate(xboxLiveToken);

        MinecraftToken minecraftToken =
                MinecraftAuthenticator.authenticate(xstsToken);

        MinecraftProfile profile =
                MinecraftProfileService.fetchProfile(minecraftToken);

        AuthenticationResult authentication =
                new AuthenticationResult(
                        AuthenticationMode.MICROSOFT,
                        profile.getName(),
                        toUuid(profile.getId()),
                        profile.getAccessToken()
                );

        return new MicrosoftLoginResult(
                authentication,
                tokenResponse.getRefreshToken()
        );
    }

    private static UUID toUuid(
            String id
    ) throws IOException {

        if (id.length() != 32) {

            throw new IOException(
                    "Identifiant de profil Minecraft invalide : "
                            + id
            );
        }

        String dashed =
                id.substring(0, 8) + "-"
                        + id.substring(8, 12) + "-"
                        + id.substring(12, 16) + "-"
                        + id.substring(16, 20) + "-"
                        + id.substring(20);

        return UUID.fromString(dashed);
    }
}
