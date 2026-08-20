package fr.deitycube.launcher.minecraft.natives;

public enum NativeClassifier {

    WINDOWS("natives-windows"),
    WINDOWS_X86("natives-windows-x86"),
    WINDOWS_ARM64("natives-windows-arm64"),

    LINUX("natives-linux"),
    MACOS("natives-osx"),

    NONE("");

    private final String name;

    NativeClassifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
