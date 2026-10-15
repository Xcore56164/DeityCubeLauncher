package fr.deitycube.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipExtractorTest {

    @TempDir
    Path tmp;

    @Test
    void extractsRegularEntries() throws IOException {

        Path zip = zipOf("payload.json", "{}", "lib/a.jar", "contenu");

        Path out = tmp.resolve("out");

        ZipExtractor.extract(zip, out, entries -> {
        });

        assertEquals("contenu", Files.readString(out.resolve("lib/a.jar")));
    }

    @Test
    void refusesEntriesEscapingTheDestination() throws IOException {

        Path out = tmp.resolve("out");

        assertThrows(IOException.class, () ->
                ZipExtractor.extract(zipOf("../evil.txt", "x"), out, entries -> {
                }));

        assertThrows(IOException.class, () ->
                ZipExtractor.extract(zipOf("lib/../../evil.txt", "x"), out, entries -> {
                }));

        assertFalse(Files.exists(tmp.resolve("evil.txt")));
    }

    @Test
    void refusesAbsoluteAndWindowsStylePaths() throws IOException {

        Path out = tmp.resolve("out");

        for (String name : new String[]{"/etc/evil", "C:/evil.txt", "lib\\..\\evil.txt"}) {

            assertThrows(IOException.class, () ->
                    ZipExtractor.extract(zipOf(name, "x"), out, entries -> {
                    }), name);
        }
    }

    private Path zipOf(String... namesAndContents) throws IOException {

        Path zip = Files.createTempFile(tmp, "test", ".zip");

        try (OutputStream out = Files.newOutputStream(zip);
             ZipOutputStream zipOut = new ZipOutputStream(out)) {

            for (int i = 0; i < namesAndContents.length; i += 2) {
                zipOut.putNextEntry(new ZipEntry(namesAndContents[i]));
                zipOut.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zipOut.closeEntry();
            }
        }

        return zip;
    }
}
