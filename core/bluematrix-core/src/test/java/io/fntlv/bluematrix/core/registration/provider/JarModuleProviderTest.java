package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.library.ModuleRuntimeLibraryLoader;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.loader.BlueClassLoaderSupport;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarModuleProviderTest {

    @TempDir
    File tempDir;

    @Test
    void emptyDirectoryDiscoversNoModules() {
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        assertTrue(provider.discoverModules().isEmpty());
    }

    @Test
    void nonJarFilesAreIgnored() throws IOException {
        Files.write(new File(tempDir, "not-a-module.txt").toPath(), new byte[]{1});
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        assertTrue(provider.discoverModules().isEmpty());
    }

    @Test
    void discoversAnnotatedModuleClassesFromJar() throws Exception {
        writeJar(new File(tempDir, "module.jar"), JarModule.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        List<ModuleCandidate> modules = provider.discoverModules();

        assertEquals(1, modules.size());
        assertEquals("jar-module", modules.get(0).getModuleInfo().id());
    }

    @Test
    void candidateScanPackagesDoNotAffectModuleDiscoveryPackage() throws Exception {
        writeJar(new File(tempDir, "module.jar"), JarModuleWithScanPackage.class, PlainClass.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        ModuleCandidate module = provider.discoverModules().stream()
                .filter(candidate -> candidate.getModuleInfo().id().equals("jar-module-with-scan-package"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("module not discovered"));

        assertEquals("jar-module-with-scan-package", module.getModuleInfo().id());
        assertTrue(module.getReflections().getSubTypesOf(ScanTarget.class).contains(PlainClass.class));
    }

    @Test
    void badJarFileDoesNotBlockOtherJars() throws Exception {
        Files.write(new File(tempDir, "bad.jar").toPath(), new byte[]{1, 2, 3});
        writeJar(new File(tempDir, "module.jar"), JarModule.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        List<ModuleCandidate> modules = provider.discoverModules();

        assertEquals(1, modules.size());
        assertEquals("jar-module", modules.get(0).getModuleInfo().id());
    }

    @Test
    void ignoresClassesThatAreNotLoadableModules() throws Exception {
        writeJar(new File(tempDir, "mixed.jar"), PlainClass.class, ModuleWithoutInfo.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        assertTrue(provider.discoverModules().isEmpty());
    }

    @Test
    void discoversMultipleModulesFromOneJar() throws Exception {
        writeJar(new File(tempDir, "multi.jar"), JarModule.class, SecondJarModule.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);

        List<String> ids = provider.discoverModules().stream()
                .map(module -> module.getModuleInfo().id())
                .collect(Collectors.toList());

        assertEquals(2, ids.size());
        assertTrue(ids.contains("jar-module"));
        assertTrue(ids.contains("second-jar-module"));
    }

    @Test
    void loadsRuntimeLibrariesBeforeReturningCandidate() throws Exception {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            writeJar(new File(tempDir, "module.jar"), JarLibraryModule.class);
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            JarModuleProvider provider = new JarModuleProvider(
                    tempDir,
                    BlueClassLoaderSupport.ensureUrlClassLoader(getClass().getClassLoader()),
                    new ModuleRuntimeLibraryLoader(libraryLoader)
            );

            List<ModuleCandidate> modules = provider.discoverModules();

            assertEquals(1, modules.size());
            assertEquals("jar-library-module", modules.get(0).getModuleInfo().id());
            assertEquals(1, downloader.calls);
            assertEquals(BlueMatrixLibraryScope.MODULE, downloader.scope);
            assertEquals("jar-library-module", downloader.qualifier);
            assertEquals("com.example:jar-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void skipsJarModuleWhenRuntimeLibraryLoadFails() throws Exception {
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw new IllegalStateException("download failed");
        });
        try {
            writeJar(new File(tempDir, "multi.jar"), JarLibraryModule.class, SecondJarModule.class);
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader());
            JarModuleProvider provider = new JarModuleProvider(
                    tempDir,
                    BlueClassLoaderSupport.ensureUrlClassLoader(getClass().getClassLoader()),
                    new ModuleRuntimeLibraryLoader(libraryLoader)
            );

            List<String> ids = provider.discoverModules().stream()
                    .map(module -> module.getModuleInfo().id())
                    .collect(Collectors.toList());

            assertEquals(1, ids.size());
            assertTrue(ids.contains("second-jar-module"));
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void discoverCreatesJarModuleCandidate() throws Exception {
        writeJar(new File(tempDir, "module.jar"), JarModule.class);
        JarModuleProvider provider = new JarModuleProvider(tempDir);
        ModuleCandidate module = provider.discoverModules().get(0);

        assertEquals(JarModule.class, module.getModuleClass());
        assertEquals("jar-module", module.getModuleInfo().id());
    }

    @Test
    void missingDirectoryIsCreatedAndDiscoversNoModules() {
        File missingDirectory = new File(tempDir, "missing");
        JarModuleProvider provider = new JarModuleProvider(missingDirectory);

        assertTrue(provider.discoverModules().isEmpty());
        assertTrue(missingDirectory.isDirectory());
    }

    @Test
    void filePathDiscoversNoModules() throws IOException {
        File filePath = new File(tempDir, "modules");
        Files.write(filePath.toPath(), new byte[]{1});
        JarModuleProvider provider = new JarModuleProvider(filePath);

        assertThrows(ModuleDiscoveryException.class, provider::discoverModules);
    }

    private static void writeJar(File jarFile, Class<?>... classes) throws IOException {
        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile))) {
            for (Class<?> clazz : classes) {
                String entryName = clazz.getName().replace('.', '/') + ".class";
                output.putNextEntry(new JarEntry(entryName));
                try (InputStream input = clazz.getClassLoader().getResourceAsStream(entryName)) {
                    if (input == null) {
                        throw new IOException("Class bytes not found: " + entryName);
                    }
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                output.closeEntry();
            }
        }
    }

    public interface ScanTarget {
    }

    public static class PlainClass implements ScanTarget {
    }

    public static class ModuleWithoutInfo implements Module {
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

    @ModuleInfo(id = "jar-module", name = "Jar Module")
    public static class JarModule implements Module {
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

    @ModuleInfo(id = "second-jar-module", name = "Second Jar Module")
    public static class SecondJarModule implements Module {
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
            id = "jar-library-module",
            name = "Jar Library Module",
            libraries = "com.example:jar-lib:1.0.0"
    )
    public static class JarLibraryModule implements Module {
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
            id = "jar-module-with-scan-package",
            name = "Jar Module With Scan Package",
            scanPackages = "io.fntlv.bluematrix.core.module.registration.provider"
    )
    public static class JarModuleWithScanPackage implements Module {
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

    @ModuleInfo(id = "private-constructor-module", name = "Private Constructor Module")
    public static class ModuleWithPrivateConstructor implements Module {
        private ModuleWithPrivateConstructor() {
        }

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
