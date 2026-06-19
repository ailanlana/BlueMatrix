package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.BlueLibraryManager;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueMatrixLibrariesTest {

    @TempDir
    File tempDir;

    @Test
    void coreLibrariesContainReflectionsRuntimeDependencies() {
        List<String> coordinates = coordinates(BlueMatrixLibraries.core());

        assertTrue(coordinates.contains("org.reflections:reflections:0.10.2"));
        assertTrue(coordinates.contains("org.javassist:javassist:3.28.0-GA"));
        assertTrue(coordinates.contains("com.google.code.findbugs:jsr305:3.0.2"));
        assertTrue(coordinates.contains("org.slf4j:slf4j-api:1.7.36"));
    }

    @Test
    void loggingLibrariesContainDefaultBackendDependencies() {
        List<String> coordinates = coordinates(BlueMatrixLibraries.logging());

        assertTrue(coordinates.contains("org.slf4j:slf4j-api:1.7.36"));
        assertTrue(coordinates.contains("ch.qos.logback:logback-core:1.2.13"));
        assertTrue(coordinates.contains("ch.qos.logback:logback-classic:1.2.13"));
    }

    @Test
    void frameworkLibrariesDeduplicateSharedDependencies() {
        List<String> coordinates = coordinates(BlueMatrixLibraries.framework());

        assertEquals(1, coordinates.stream()
                .filter("org.slf4j:slf4j-api:1.7.36"::equals)
                .count());
    }

    @Test
    void loadFrameworkUsesManager() {
        RecordingLibraryManager manager = new RecordingLibraryManager(tempDir);

        BlueMatrixLibraries.loadFramework(manager);

        assertTrue(manager.loadedCoordinates.contains("org.reflections:reflections:0.10.2"));
        assertTrue(manager.loadedCoordinates.contains("ch.qos.logback:logback-classic:1.2.13"));
    }

    private static List<String> coordinates(List<BlueLibrary> libraries) {
        return libraries.stream().map(BlueLibrary::toString).collect(Collectors.toList());
    }

    private static class RecordingLibraryManager extends BlueLibraryManager {
        private final List<String> loadedCoordinates = Collections.synchronizedList(new ArrayList<>());

        private RecordingLibraryManager(File rootFolder) {
            super("test", rootFolder, "libs", new URLClassLoader(new URL[0]));
        }

        @Override
        public void addMavenCentral() {
        }

        @Override
        protected void loadSingleLibrary(BlueLibrary library) {
            loadedCoordinates.add(library.toString());
        }
    }
}
