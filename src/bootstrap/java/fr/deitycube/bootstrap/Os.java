package fr.deitycube.bootstrap;

enum Os {

    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    static Os current() {

        String name = System.getProperty("os.name", "").toLowerCase();

        if (name.contains("win")) {
            return WINDOWS;
        }

        if (name.contains("mac")) {
            return MACOS;
        }

        if (name.contains("linux")) {
            return LINUX;
        }

        return UNKNOWN;
    }

    /**
     * Clé de cette plateforme dans le bloc "payload" du manifeste, ou {@code null} si aucun
     * payload n'est publié pour elle.
     */
    String payloadKey() {

        return switch (this) {
            case WINDOWS -> "windows";
            case LINUX -> "linux";
            default -> null;
        };
    }
}
