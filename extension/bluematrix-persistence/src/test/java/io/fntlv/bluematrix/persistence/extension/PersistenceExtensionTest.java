package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceExtensionTest {
    @TempDir
    File tempDir;

    @Test
    void registersPersistenceResolvers() {
        BlueMatrixLibraryLoader.downloaderForTesting(new BlueMatrixLibraryLoader.Downloader() {
            @Override
            public void download(BlueMatrixLibraryLoader bootstrap,
                                 File dataFolder,
                                 ClassLoader classLoader,
                                 BlueMatrixLibraryScope scope,
                                 String qualifier,
                                 BlueLibrary library) {
            }
        });
        try {
            BlueMatrixContainer container = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            assertTrue(hasResolver(container, PersistenceContextResolver.class));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    private static boolean hasResolver(BlueMatrixContainer container,
                                       Class<? extends ModuleParameterResolver> resolverType) {
        for (ModuleParameterResolver resolver : container.getParameterResolvers().resolvers()) {
            if (resolverType.isInstance(resolver)) {
                return true;
            }
        }
        return false;
    }
}
