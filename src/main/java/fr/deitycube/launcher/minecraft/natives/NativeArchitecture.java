package fr.deitycube.launcher.minecraft.natives;

public enum NativeArchitecture {

    X86("x86"),
    X64("x86_64"),
    ARM64("arm64"),
    UNKNOWN("unknown");

    private final String name;

    NativeArchitecture(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static NativeArchitecture current() {

        String architecture =
                System.getProperty("os.arch")
                        .toLowerCase();

        return switch (architecture) {

            case "x86", "i386", "i486", "i586", "i686" ->
                    X86;

            case "amd64", "x86_64" ->
                    X64;

            case "aarch64", "arm64" ->
                    ARM64;

            default ->
                    UNKNOWN;
        };
    }
}
