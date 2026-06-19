package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarModuleProvider implements ModuleProvider {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(JarModuleProvider.class);

    private final File jarDirectory;

    public JarModuleProvider(File jarDirectory) {
        this.jarDirectory = jarDirectory;
    }

    @Override
    public List<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
        if (!ensureJarDirectory()) {
            return discoveredModules;
        }

        File[] jarFiles = jarDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return discoveredModules;
        }

        Arrays.sort(jarFiles, Comparator.comparing(File::getName));
        for (File jarFile : jarFiles) {
            try {
                discoveredModules.addAll(discoverModulesInJar(jarFile));
            } catch (ModuleDiscoveryException e) {
                LOGGER.warn(
                        "Skipping jar module file: {} - {}",
                        jarFile.getName(),
                        e.getMessage()
                );
            }
        }
        return discoveredModules;
    }

    private boolean ensureJarDirectory() {
        if (!jarDirectory.exists()) {
            if (jarDirectory.mkdirs()) {
                return true;
            }
            throw new ModuleDiscoveryException("Failed to create jar module directory: " + jarDirectory.getAbsolutePath());
        }
        if (!jarDirectory.isDirectory()) {
            throw new ModuleDiscoveryException("Jar module path is not a directory: " + jarDirectory.getAbsolutePath());
        }
        return true;
    }

    private List<ModuleCandidate> discoverModulesInJar(File jarFile) {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    JarModuleProvider.class.getClassLoader()
            );
            try (JarFile jar = new JarFile(jarFile)) {
                List<JarEntry> classEntries = new ArrayList<>();
                jar.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().endsWith(".class"))
                        .forEach(classEntries::add);
                classEntries.sort(Comparator.comparing(JarEntry::getName));

                for (JarEntry entry : classEntries) {
                    discoverModuleClass(jarFile, classLoader, entry).ifPresent(discoveredModules::add);
                }
            }
        } catch (Exception e) {
            throw new ModuleDiscoveryException("Failed to discover modules from jar: " + jarFile.getAbsolutePath(), e);
        }
        return discoveredModules;
    }

    private java.util.Optional<ModuleCandidate> discoverModuleClass(File jarFile, ClassLoader classLoader, JarEntry entry) {
        String className = toClassName(entry.getName());
        try {
            Class<?> clazz = classLoader.loadClass(className);
            if (!isModuleClass(clazz)) {
                return java.util.Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Class<? extends Module> moduleClass = (Class<? extends Module>) clazz;
            ModuleInfo info = moduleClass.getAnnotation(ModuleInfo.class);
            ModuleCandidate candidate = new ModuleCandidate(moduleClass, info);
            return java.util.Optional.of(candidate);
        } catch (Throwable e) {
            ModuleDiscoveryException exception = new ModuleDiscoveryException(
                    "Failed to inspect class in jar: " + jarFile.getName() + " -> " + className,
                    e
            );
            LOGGER.warn(
                    "Failed to inspect class in jar: {} -> {} - {}",
                    jarFile.getName(),
                    className,
                    exception.getMessage()
            );
            return java.util.Optional.empty();
        }
    }

    private boolean isModuleClass(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        return Module.class.isAssignableFrom(clazz)
                && clazz.isAnnotationPresent(ModuleInfo.class)
                && !clazz.isInterface()
                && !Modifier.isAbstract(modifiers);
    }

    private String toClassName(String entryName) {
        return entryName
                .substring(0, entryName.length() - ".class".length())
                .replace('/', '.');
    }
}
