package fr.deitycube.launcher.minecraft.natives;

import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;

public final class NativeDetector {

    private NativeDetector() {
    }

    public static boolean isWindowsNative(
            MinecraftLibrary library
    ) {

        if (library.getName() == null) {
            return false;
        }

        String name =
                library.getName().toLowerCase();

        if (!name.contains("natives-windows")) {
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
}
