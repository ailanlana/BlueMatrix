package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleContext.ModuleState;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleLibraryLoadListenerTest {
    @TempDir
    File tempDir;

    @Test
    void downloadsModuleLibraries() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);

            listener.onRegisterPost(new ModuleRegisterEvent.Post(context(new LibraryModule())));

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
            BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);

            listener.onRegisterPost(new ModuleRegisterEvent.Post(context(new RepositoryLibraryModule())));

            List<String> repositories = bootstrap.repositoriesForTesting(BlueMatrixLibraryScope.MODULE, "repository-library-module");
            assertTrue(repositories.contains("https://repo.example.com/releases"));
            assertEquals(1, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void moduleRepositoriesDoNotAffectOtherModules() {
        BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
        bootstrap.addModuleRepository("first-module", "https://repo.example.com/releases");

        assertTrue(bootstrap.repositoriesForTesting(BlueMatrixLibraryScope.MODULE, "first-module")
                .contains("https://repo.example.com/releases"));
        assertEquals(0, bootstrap.repositoriesForTesting(BlueMatrixLibraryScope.MODULE, "second-module").size());
    }

    @Test
    void globalRepositoriesApplyToAllScopes() {
        BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
        bootstrap.addRepository("https://repo.example.com/public");

        assertTrue(bootstrap.repositoriesForTesting(BlueMatrixLibraryScope.APP, null)
                .contains("https://repo.example.com/public"));
        assertTrue(bootstrap.repositoriesForTesting(BlueMatrixLibraryScope.MODULE, "library-module")
                .contains("https://repo.example.com/public"));
    }

    @Test
    void skipsModulesWithoutLibraries() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);

            listener.onRegisterPost(new ModuleRegisterEvent.Post(context(new EmptyLibraryModule())));

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
            BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);

            listener.onRegisterPost(new ModuleRegisterEvent.Post(context(new DuplicateLibraryModule())));

            assertEquals(1, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void downloadFailureMarksModuleError() {
        RuntimeException failure = new RuntimeException("download failed");
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw failure;
        });
        try {
            BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);
            ModuleContext context = context(new LibraryModule());

            listener.onRegisterPost(new ModuleRegisterEvent.Post(context));

            assertEquals(ModuleState.ERROR, context.getModuleState());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void blankRepositoryMarksModuleError() {
        BlueMatrixLibraryLoader bootstrap = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
        ModuleLibraryLoadListener listener = new ModuleLibraryLoadListener(bootstrap);
        ModuleContext context = context(new BlankRepositoryModule());

        listener.onRegisterPost(new ModuleRegisterEvent.Post(context));

        assertEquals(ModuleState.ERROR, context.getModuleState());
    }

    private static ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
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
