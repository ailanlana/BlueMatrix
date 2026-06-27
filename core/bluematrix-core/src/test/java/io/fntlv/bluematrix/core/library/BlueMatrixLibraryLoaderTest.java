package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueMatrixLibraryLoaderTest {
    @TempDir
    File tempDir;

    @Test
    void loadsCoreLibrariesWhenJavassistIsMissing() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            ClassLoader classLoader = new HiddenClassLoader(getClass().getClassLoader(), "javassist.");
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, classLoader);

            libraryLoader.loadCoreLibraries();

            assertTrue(downloader.coordinates.contains("org.javassist:javassist:3.28.0-GA"));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    private static final class RecordingDownloader implements BlueMatrixLibraryLoader.Downloader {
        private final List<String> coordinates = new ArrayList<>();

        @Override
        public void download(
                BlueMatrixLibraryLoader loader,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        ) {
            coordinates.add(library.toString());
        }
    }

    private static final class HiddenClassLoader extends ClassLoader {
        private final String hiddenPrefix;

        private HiddenClassLoader(ClassLoader parent, String hiddenPrefix) {
            super(parent);
            this.hiddenPrefix = hiddenPrefix;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(hiddenPrefix)) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
