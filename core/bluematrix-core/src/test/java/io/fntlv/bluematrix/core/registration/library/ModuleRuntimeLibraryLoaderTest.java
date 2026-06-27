package io.fntlv.bluematrix.core.module.registration.library;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

            ModuleRuntimeLibraryLoadResult result = runtimeLibraryLoader.load(descriptor(LibraryModule.class));

            assertTrue(result.success());
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

            runtimeLibraryLoader.load(descriptor(RepositoryLibraryModule.class));

            List<String> repositories = repositoriesForTesting(libraryLoader, "repository-library-module");
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

            ModuleRuntimeLibraryLoadResult result = runtimeLibraryLoader.load(descriptor(EmptyLibraryModule.class));

            assertTrue(result.success());
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

            runtimeLibraryLoader.load(descriptor(DuplicateLibraryModule.class));

            assertEquals(1, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void downloadFailureReturnsFailedLibraryResult() {
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw new IllegalStateException("download failed");
        });
        try {
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

            ModuleRuntimeLibraryLoadResult result = runtimeLibraryLoader.load(descriptor(LibraryModule.class));

            assertFalse(result.success());
            assertTrue(result.failed());
            assertEquals("library-module", result.moduleId());
            assertEquals(1, result.failures().size());
            assertEquals("com.example:module-lib:1.0.0", result.failedLibraries().get(0));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void blankRepositoryReturnsFailureResult() {
        BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
        ModuleRuntimeLibraryLoader runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);

        ModuleRuntimeLibraryLoadResult result = runtimeLibraryLoader.load(descriptor(BlankRepositoryModule.class));

        assertFalse(result.success());
        assertEquals(1, result.failures().size());
        assertTrue(result.failedLibraries().isEmpty());
    }

    private static ModuleDescriptor descriptor(Class<? extends Module> moduleClass) {
        return ModuleDescriptor.from(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    @SuppressWarnings("unchecked")
    private static List<String> repositoriesForTesting(BlueMatrixLibraryLoader libraryLoader, String moduleId) {
        try {
            Method method = BlueMatrixLibraryLoader.class.getDeclaredMethod(
                    "repositoriesForTesting",
                    BlueMatrixLibraryScope.class,
                    String.class
            );
            method.setAccessible(true);
            return (List<String>) method.invoke(libraryLoader, BlueMatrixLibraryScope.MODULE, moduleId);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect module repositories", e);
        }
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
