package fr.deitycube.launcher.minecraft.natives;

import fr.deitycube.launcher.filesystem.GameDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class NativeExtractor {

    private NativeExtractor() {
    }

    public static void extract(
            Path nativeJar,
            String minecraftVersion
    ) throws IOException {

        Path outputDirectory =
                GameDirectory.getNativesDirectory(
                        minecraftVersion
                );

        Files.createDirectories(outputDirectory);

        System.out.println(
                "Extraction des natives : "
                        + nativeJar.getFileName()
        );

        try (
                InputStream inputStream =
                        Files.newInputStream(nativeJar);

                ZipInputStream zipInputStream =
                        new ZipInputStream(inputStream)
        ) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    continue;
                }

                if (entryName.startsWith("META-INF/")) {
                    continue;
                }

                if (!entryName.toLowerCase().endsWith(".dll")) {
                    continue;
                }

                String fileName =
                        Path.of(entryName)
                                .getFileName()
                                .toString();

                Path destination =
                        outputDirectory
                                .resolve(fileName)
                                .normalize();

                if (!destination.startsWith(
                        outputDirectory.normalize()
                )) {
                    throw new IOException(
                            "Entrée ZIP dangereuse : "
                                    + entryName
                    );
                }

                try (OutputStream outputStream =
                             Files.newOutputStream(destination)) {

                    zipInputStream.transferTo(outputStream);
                }

                System.out.println(
                        "  Extrait : " + fileName
                );
            }
        }
    }
}
