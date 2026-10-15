package fr.deitycube.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsTest {

    @Test
    void comparesNumerically() {

        assertTrue(Versions.isNewer("1.0.10", "1.0.9"));
        assertTrue(Versions.isNewer("1.1.0", "1.0.99"));
        assertTrue(Versions.isNewer("2.0", "1.9.9"));
        assertFalse(Versions.isNewer("1.0.0", "1.0.0"));
        assertFalse(Versions.isNewer("1.0.0", "1.0"));
        assertEquals(0, Versions.compare("1.2", "1.2.0"));
    }

    @Test
    void acceptsOnlyPlainDottedNumbers() {

        assertTrue(Versions.isValid("1.0.0"));
        assertTrue(Versions.isValid("12.34"));

        assertFalse(Versions.isValid(null));
        assertFalse(Versions.isValid(""));
        assertFalse(Versions.isValid("1"));
        assertFalse(Versions.isValid("1.0.6-dev"));
        assertFalse(Versions.isValid("v1.0.0"));
        assertFalse(Versions.isValid("1.0.0.0.0"));
    }

    @Test
    void rejectsVersionsThatCouldEscapeTheVersionsFolder() {

        assertFalse(Versions.isValid("../1.0.0"));
        assertFalse(Versions.isValid("..\\1.0.0"));
        assertFalse(Versions.isValid("1.0.0/../../x"));
        assertFalse(Versions.isValid("1.0.0\n"));
    }
}
