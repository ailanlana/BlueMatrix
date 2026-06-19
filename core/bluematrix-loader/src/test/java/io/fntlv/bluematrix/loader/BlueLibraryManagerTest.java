package io.fntlv.bluematrix.loader;

import io.fntlv.bluematrix.loader.BlueMatrixLoaderException;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlueLibraryManagerTest {

    @TempDir
    File tempDir;

    @Test
    void constructorRequiresUrlClassLoader() {
        ClassLoader classLoader = new ClassLoader() {
        };

        assertThrows(BlueMatrixLoaderException.class,
                () -> new BlueLibraryManager("test", tempDir, "libs", classLoader));
    }

    @Test
    void emptyLibraryCollectionReturnsWithoutLoading() {
        RecordingLibraryManager manager = new RecordingLibraryManager(tempDir);

        manager.loadLibraries(Collections.emptyList());

        assertEquals(0, manager.loadedLibraries.size());
    }

    @Test
    void batchLoadingContinuesWhenOneLibraryFails() {
        RecordingLibraryManager manager = new RecordingLibraryManager(tempDir);
        List<BlueLibrary> libraries = Arrays.asList(
                BlueLibraryFactory.of("org.example:first:1.0.0"),
                BlueLibraryFactory.of("org.example:fail:1.0.0"),
                BlueLibraryFactory.of("org.example:last:1.0.0")
        );

        manager.loadLibraries(libraries);

        assertEquals(3, manager.loadedLibraries.size());
    }

    private static class RecordingLibraryManager extends BlueLibraryManager {
        private final List<String> loadedLibraries = new CopyOnWriteArrayList<>();

        private RecordingLibraryManager(File rootFolder) {
            super("test", rootFolder, "libs", new URLClassLoader(new URL[0]));
        }

        @Override
        protected void loadSingleLibrary(BlueLibrary library) {
            loadedLibraries.add(library.getArtifactId());
            if ("fail".equals(library.getArtifactId())) {
                throw new IllegalStateException("Expected failure");
            }
        }
    }
}
