package io.fntlv.bluematrix.core.registration.provider;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.library.ModuleRuntimeLibraryLoader;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.provider.PackageModuleProvider;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageModuleProviderTest {
    private static final String FIXTURE_PACKAGE =
            "io.fntlv.bluematrix.core.registration.provider.packagefixtures";

    @TempDir
    File tempDir;

    @Test
    void discoversPackageModules() {
        PackageModuleProvider provider = new PackageModuleProvider(FIXTURE_PACKAGE);

        List<String> ids = ids(provider.discoverModules());

        assertTrue(ids.contains("package-plain-module"));
        assertTrue(ids.contains("package-library-module"));
    }

    @Test
    void loadsRuntimeLibrariesBeforeReturningCandidate() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            PackageModuleProvider provider = new PackageModuleProvider(
                    FIXTURE_PACKAGE,
                    new ModuleRuntimeLibraryLoader(libraryLoader)
            );

            List<String> ids = ids(provider.discoverModules());

            assertTrue(ids.contains("package-library-module"));
            assertEquals(1, downloader.calls);
            assertEquals(BlueMatrixLibraryScope.MODULE, downloader.scope);
            assertEquals("package-library-module", downloader.qualifier);
            assertEquals("com.example:package-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void skipsPackageModuleWhenRuntimeLibraryLoadFails() {
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw new IllegalStateException("download failed");
        });
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            PackageModuleProvider provider = new PackageModuleProvider(
                    FIXTURE_PACKAGE,
                    new ModuleRuntimeLibraryLoader(libraryLoader)
            );

            List<String> ids = ids(provider.discoverModules());

            assertTrue(ids.contains("package-plain-module"));
            assertFalse(ids.contains("package-library-module"));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    private static List<String> ids(List<ModuleCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.id())
                .collect(Collectors.toList());
    }

    private static final class RecordingDownloader implements BlueMatrixLibraryLoader.Downloader {
        private int calls;
        private BlueMatrixLibraryScope scope;
        private String qualifier;
        private BlueLibrary library;

        @Override
        public void download(
                BlueMatrixLibraryLoader bootstrap,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        ) {
            calls++;
            this.scope = scope;
            this.qualifier = qualifier;
            this.library = library;
        }
    }
}
