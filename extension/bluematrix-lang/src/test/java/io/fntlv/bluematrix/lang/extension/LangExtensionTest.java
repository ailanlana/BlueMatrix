package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangExtensionTest {
    @TempDir
    File tempDir;

    @Test
    void applyOnlyRegistersLangModuleListener() {
        RecordingBootstrap bootstrap = new RecordingBootstrap(tempDir);

        new LangExtension().apply(bootstrap, null);

        assertEquals(0, bootstrap.repositories);
        assertEquals(0, bootstrap.extensionLibraries);
        assertEquals(0, bootstrap.parameterResolvers);
        assertEquals(1, bootstrap.eventListeners);
        assertTrue(bootstrap.lastEventListener instanceof LangModuleListener);
    }

    private static final class RecordingBootstrap implements BlueMatrixExtensionBootstrap {
        private final File dataFolder;
        private int repositories;
        private int extensionLibraries;
        private int parameterResolvers;
        private int eventListeners;
        private Object lastEventListener;

        private RecordingBootstrap(File dataFolder) {
            this.dataFolder = dataFolder;
        }

        @Override
        public File dataFolder() {
            return dataFolder;
        }

        @Override
        public BlueMatrixExtensionBootstrap repository(String repositoryUrl) {
            repositories++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates) {
            extensionLibraries++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library) {
            extensionLibraries++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates, String presenceClass) {
            extensionLibraries++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
            extensionLibraries++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap parameterResolver(ModuleParameterResolver resolver) {
            parameterResolvers++;
            return this;
        }

        @Override
        public BlueMatrixExtensionBootstrap eventListener(Object listener) {
            eventListeners++;
            lastEventListener = listener;
            return this;
        }
    }
}
