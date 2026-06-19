package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigExtensionTest {
    @TempDir
    File tempDir;

    @Test
    void registersConfigModuleListener() {
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
        });
        try {
            BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            blueMatrixContainer.getEventBus().publish(new ModuleLoadEvent.Pre(new ModuleContext(
                    new ExtensionModule(),
                    ExtensionModule.class.getAnnotation(ModuleInfo.class)
            )));

            assertTrue(new File(tempDir, "modules/extension-module/config.yml").exists());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void skipsConfigExtensionLibrariesAlreadyPresentOnClasspath() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            assertEquals(0, downloader.libraries.size());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @ModuleInfo(id = "extension-module", name = "Extension Module")
    private static final class ExtensionModule implements Module {
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

    @ConfigRegister
    @BlueConfig(category = "database")
    private static final class DatabaseConfig {
        @BlueConfig.Field(path = "host", defaultValue = "localhost")
        private String host;
    }

    private static final class RecordingDownloader implements BlueMatrixLibraryLoader.Downloader {
        private final List<String> libraries = new ArrayList<>();

        @Override
        public void download(
                BlueMatrixLibraryLoader bootstrap,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        ) {
            if (scope == BlueMatrixLibraryScope.EXTENSION) {
                libraries.add(qualifier + ":" + library);
            }
        }
    }
}
