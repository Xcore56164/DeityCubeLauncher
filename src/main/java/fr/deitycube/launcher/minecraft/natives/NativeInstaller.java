package fr.deitycube.launcher.minecraft.natives;

import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.library.MinecraftLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NativeInstaller {

    public void installNatives(
            List<MinecraftLibrary> libraries,
            String minecraftVersion
    ) throws IOException {

        int extracted = 0;
        int ignored = 0;

        for (MinecraftLibrary library : libraries) {

            if (!NativeDetector.isWindowsNative(library)) {
                continue;
            }

            if (library.getDownloads() == null
                    || library.getDownloads().getArtifact() == null) {

                ignored++;
                continue;
            }

            MinecraftLibrary.Artifact artifact =
                    library.getDownloads()
                            .getArtifact();

            Path nativeJar =
                    GameDirectory.getLibrariesDirectory()
                            .resolve(artifact.getPath());

            if (!Files.exists(nativeJar)) {

                throw new IOException(
                        "Native introuvable : "
                                + nativeJar
                );
            }

            NativeExtractor.extract(
                    nativeJar,
                    minecraftVersion
            );

            extracted++;
        }

        System.out.println();
        System.out.println("=================================");
        System.out.println("          NATIVES MINECRAFT");
        System.out.println("=================================");
        System.out.println(
                "Archives extraites : " + extracted
        );
        System.out.println(
                "Ignorées            : " + ignored
        );
    }
}
