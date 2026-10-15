package fr.deitycube.bootstrap;

/**
 * Ce que le bootstrap lit dans launcher_manifest.json. CONTRAT FIGÉ : ces clés ne changent plus ;
 * le manifeste peut en contenir d'autres (les clés "windows", "linux_deb"... de l'ancien système
 * y restent pour faire migrer les anciens launchers), elles sont ignorées ici.
 *
 * @param payload payload de la plateforme courante, ou {@code null} s'il n'y en a pas de publié
 */
record UpdateManifest(String version, String notes, int protocol, PayloadTarget payload) {

    record PayloadTarget(String url, String sha256, String signature) {
    }
}
