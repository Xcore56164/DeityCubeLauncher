package fr.deitycube.launcher.minecraft.natives;

import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;
import fr.deitycube.launcher.minecraft.platform.OperatingSystem;

public final class NativeDetector {

    private NativeDetector() {
    }

    public static boolean isNativeForCurrentPlatform(
            MinecraftLibrary library
    ) {

        return switch (OperatingSystem.current()) {
            case WINDOWS -> isWindowsNative(library);
            case LINUX -> isLinuxNative(library);
            case MACOS -> isMacNative(library);
            case UNKNOWN -> false;
        };
    }

    public static boolean isWindowsNative(
            MinecraftLibrary library
    ) {

        String name = lowerCaseName(library);

        if (name == null || !name.contains("natives-windows")) {
            return false;
        }

        NativeArchitecture architecture =
                NativeArchitecture.current();

        return switch (architecture) {

            case X64 ->
                    !name.contains("natives-windows-x86")
                            && !name.contains("natives-windows-arm64");

            case X86 ->
                    name.contains("natives-windows-x86");

            case ARM64 ->
                    name.contains("natives-windows-arm64");

            case UNKNOWN ->
                    false;
        };
    }

    public static boolean isLinuxNative(
            MinecraftLibrary library
    ) {

        String name = lowerCaseName(library);

        if (name == null || !name.contains("natives-linux")) {
            return false;
        }

        NativeArchitecture architecture =
                NativeArchitecture.current();

        return switch (architecture) {

            case X64 ->
                    !name.contains("natives-linux-arm64")
                            && !name.contains("natives-linux-arm32");

            case ARM64 ->
                    name.contains("natives-linux-arm64");

            case X86, UNKNOWN ->
                    false;
        };
    }

    public static boolean isMacNative(
            MinecraftLibrary library
    ) {

        String name = lowerCaseName(library);

        // Selon la version de Minecraft, Mojang republie LWJGL sous "natives-osx"
        // (versions plus anciennes) ou "natives-macos" (versions récentes).
        if (name == null
                || !(name.contains("natives-osx") || name.contains("natives-macos"))) {
            return false;
        }

        NativeArchitecture architecture =
                NativeArchitecture.current();

        return switch (architecture) {

            case ARM64 ->
                    name.contains("arm64");

            case X64 ->
                    !name.contains("arm64");

            case X86, UNKNOWN ->
                    false;
        };
    }

    private static String lowerCaseName(MinecraftLibrary library) {

        return library.getName() == null
                ? null
                : library.getName().toLowerCase();
    }
}
