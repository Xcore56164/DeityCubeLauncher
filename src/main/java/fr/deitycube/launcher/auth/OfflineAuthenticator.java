package fr.deitycube.launcher.auth;

public final class OfflineAuthenticator {

    private OfflineAuthenticator() {
    }

    public static AuthenticationResult authenticate(
            String username
    ) {

        if (username == null
                || username.isBlank()) {

            throw new IllegalArgumentException(
                    "Le pseudo ne peut pas être vide."
            );
        }

        String cleanUsername =
                username.trim();

        if (cleanUsername.length() > 16) {

            throw new IllegalArgumentException(
                    "Le pseudo ne peut pas dépasser 16 caractères."
            );
        }

        OfflineProfile profile =
                new OfflineProfile(cleanUsername);

        return new AuthenticationResult(
                AuthenticationMode.OFFLINE,
                profile.getName(),
                profile.getUuid(),
                "0"
        );
    }
}
