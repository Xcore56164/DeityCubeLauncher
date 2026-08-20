package fr.deitycube.launcher.minecraft.platform;

public enum OperatingSystem {

    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    public static OperatingSystem current() {

        String osName =
                System.getProperty("os.name")
                        .toLowerCase();

        if (osName.contains("win")) {
            return WINDOWS;
        }

        if (osName.contains("mac")) {
            return MACOS;
        }

        if (osName.contains("linux")) {
            return LINUX;
        }

        return UNKNOWN;
    }

    public String minecraftName() {

        return switch (this) {
            case WINDOWS -> "windows";
            case LINUX -> "linux";
            case MACOS -> "osx";
            case UNKNOWN -> "unknown";
        };
    }
}
