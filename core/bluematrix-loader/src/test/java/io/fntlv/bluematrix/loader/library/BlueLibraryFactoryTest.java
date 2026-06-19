package io.fntlv.bluematrix.loader.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueLibraryFactoryTest {

    @Test
    void parsesMavenCoordinates() {
        BlueLibrary library = BlueLibraryFactory.of("org.example:demo:1.0.0");

        assertEquals("org.example", library.getGroupId());
        assertEquals("demo", library.getArtifactId());
        assertEquals("1.0.0", library.getVersion());
        assertFalse(library.hasChecksum());
    }

    @Test
    void parsesMavenCoordinatesWithChecksum() {
        BlueLibrary library = BlueLibraryFactory.of("org.example:demo:1.0.0:checksum");

        assertEquals("org.example", library.getGroupId());
        assertEquals("demo", library.getArtifactId());
        assertEquals("1.0.0", library.getVersion());
        assertEquals("checksum", library.getChecksum());
        assertTrue(library.hasChecksum());
    }

    @Test
    void rejectsCoordinatesWithoutVersion() {
        assertThrows(IllegalArgumentException.class, () -> BlueLibraryFactory.of("org.example:demo"));
    }

    @Test
    void relocatesLibraryPackages() {
        BlueLibrary library = BlueLibraryFactory.of("org.example:demo:1.0.0")
                .relocate("org.source", "io.fntlv.libs.source")
                .relocate("org.second", "io.fntlv.libs.second");

        assertTrue(library.hasRelocations());
        assertEquals(2, library.getRelocations().size());
        assertEquals("org.source", library.getRelocations().get(0).getPattern());
        assertEquals("io.fntlv.libs.source", library.getRelocations().get(0).getRelocatedPattern());
        assertEquals("org.second", library.getRelocations().get(1).getPattern());
        assertEquals("io.fntlv.libs.second", library.getRelocations().get(1).getRelocatedPattern());
        assertEquals("org.example:demo:1.0.0", library.toString());
    }

    @Test
    void rejectsBlankRelocationPatterns() {
        BlueLibrary library = BlueLibraryFactory.of("org.example:demo:1.0.0");

        assertThrows(IllegalArgumentException.class, () -> library.relocate(" ", "io.fntlv.libs.demo"));
        assertThrows(IllegalArgumentException.class, () -> library.relocate("org.example", " "));
    }
}
