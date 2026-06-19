package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
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
}
