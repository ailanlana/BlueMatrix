package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleRuntimeLibraryLoaderTest {
    @TempDir
    File tempDir;

    @Test
    void loadsModuleLibraries() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            runtimeLibraryLoader.load(LibraryModule.class.getAnnotation(ModuleInfo.class));

            assertEquals(1, downloader.calls);
            assertEquals(BlueMatrixLibraryScope.MODULE, downloader.scope);
            assertEquals("library-module", downloader.qualifier);
            assertEquals("com.example:module-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void registersModuleRepositories() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            runtimeLibraryLoader.load(RepositoryLibraryModule.class.getAnnotation(ModuleInfo.class));

            List<String> repositories = libraryLoader.repositoriesForTesting(
                    BlueMatrixLibraryScope.MODULE,
                    "repository-library-module"
            );
            assertTrue(repositories.contains("https://repo.example.com/releases"));
            assertEquals(1, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void skipsModulesWithoutLibraries() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            runtimeLibraryLoader.load(EmptyLibraryModule.class.getAnnotation(ModuleInfo.class));

            assertEquals(0, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void duplicateModuleLibrariesDownloadOnce() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            runtimeLibraryLoader.load(DuplicateLibraryModule.class.getAnnotation(ModuleInfo.class));

            assertEquals(1, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void downloadFailureThrowsModuleRuntimeLibraryException() {
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw new IllegalStateException("download failed");
        });
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            assertThrows(ModuleRuntimeLibraryException.class,
                    () -> runtimeLibraryLoader.load(LibraryModule.class.getAnnotation(ModuleInfo.class)));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void blankRepositoryThrowsModuleRuntimeLibraryException() {
        BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
        ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

        assertThrows(ModuleRuntimeLibraryException.class,
                () -> runtimeLibraryLoader.load(BlankRepositoryModule.class.getAnnotation(ModuleInfo.class)));
    }

    @ModuleInfo(id = "library-module", name = "Library Module", libraries = "com.example:module-lib:1.0.0")
    private static class LibraryModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "repository-library-module",
            name = "Repository Library Module",
            libraries = "com.example:module-lib:1.0.0",
            repositories = "https://repo.example.com/releases"
    )
    private static class RepositoryLibraryModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "empty-library-module", name = "Empty Library Module")
    private static class EmptyLibraryModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "duplicate-library-module",
            name = "Duplicate Library Module",
            libraries = {
                    "com.example:module-lib:1.0.0",
                    "com.example:module-lib:1.0.0"
            }
    )
    private static class DuplicateLibraryModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "blank-repository-module",
            name = "Blank Repository Module",
            libraries = "com.example:module-lib:1.0.0",
            repositories = " "
    )
    private static class BlankRepositoryModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
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
