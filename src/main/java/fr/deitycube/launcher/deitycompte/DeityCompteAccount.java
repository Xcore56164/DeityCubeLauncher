package fr.deitycube.launcher.deitycompte;

import com.fasterxml.jackson.databind.JsonNode;

/** Profil du DeityCompte connecté, tel que renvoyé par l'API ("compte"). */
public final class DeityCompteAccount {

    private final String pseudo;
    private final String avatarUrl;
    private final String role;
    private final String microsoftGamertag;
    private final boolean microsoftRelinkRequired;
    private final String minecraftName;
    private final boolean minecraftServicesEnabled;

    private DeityCompteAccount(
            String pseudo,
            String avatarUrl,
            String role,
            String microsoftGamertag,
            boolean microsoftRelinkRequired,
            String minecraftName,
            boolean minecraftServicesEnabled
    ) {
        this.pseudo = pseudo;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.microsoftGamertag = microsoftGamertag;
        this.microsoftRelinkRequired = microsoftRelinkRequired;
        this.minecraftName = minecraftName;
        this.minecraftServicesEnabled = minecraftServicesEnabled;
    }

    static DeityCompteAccount fromJson(JsonNode json) {

        JsonNode microsoft = json.path("microsoft");
        JsonNode minecraft = json.path("minecraft");

        return new DeityCompteAccount(
                json.path("pseudo").asText(""),
                json.hasNonNull("avatar") ? json.get("avatar").asText() : null,
                json.path("role").asText(""),
                microsoft.isObject() ? microsoft.path("gamertag").asText(null) : null,
                microsoft.path("reconnexion_requise").asBoolean(false),
                minecraft.isObject() ? minecraft.path("nom").asText(null) : null,
                json.path("minecraft_services").asBoolean(false)
        );
    }

    public String getPseudo() {
        return pseudo;
    }

    /** URL de la photo de profil (PNG), ou null si le joueur n'en a pas. */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public String getMicrosoftGamertag() {
        return microsoftGamertag;
    }

    public boolean isMicrosoftRelinkRequired() {
        return microsoftRelinkRequired;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public boolean isMinecraftServicesEnabled() {
        return minecraftServicesEnabled;
    }

}
