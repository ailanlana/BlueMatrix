package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoader;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import io.fntlv.bluematrix.core.module.registration.issue.issues.RuntimeLibraryLoadFailedIssue;
import io.fntlv.bluematrix.loader.BlueClassLoaderSupport;
import io.fntlv.bluematrix.loader.BlueMatrixLoaderException;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarModuleProvider implements ModuleProvider {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(JarModuleProvider.class);

    private final File jarDirectory;
    private final ClassLoader classLoader;
    private final Set<File> loadedJarFiles = new HashSet<>();
    private final ModuleRuntimeLibraryLoader runtimeLibraryLoader;
    private final JarModuleMetadataReader metadataReader = new JarModuleMetadataReader();

    public JarModuleProvider(File jarDirectory) {
        this(jarDirectory, BlueClassLoaderSupport.ensureUrlClassLoader(JarModuleProvider.class.getClassLoader()));
    }

    public JarModuleProvider(File jarDirectory, ClassLoader classLoader) {
        this(jarDirectory, classLoader, null);
    }

    public JarModuleProvider(File jarDirectory, ClassLoader classLoader, ModuleRuntimeLibraryLoader runtimeLibraryLoader) {
        if (jarDirectory == null) {
            throw new IllegalArgumentException("jarDirectory cannot be null");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        this.jarDirectory = jarDirectory;
        this.classLoader = classLoader;
        this.runtimeLibraryLoader = runtimeLibraryLoader;
    }

    @Override
    public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        if (!ensureJarDirectory()) {
            return ModuleRegistrationStageResult.of(discoveredModules);
        }

        File[] jarFiles = jarDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return ModuleRegistrationStageResult.of(discoveredModules);
        }

        Arrays.sort(jarFiles, Comparator.comparing(File::getName));
        for (File jarFile : jarFiles) {
            try {
                ModuleRegistrationStageResult<ModuleCandidate> result = discoverModulesInJar(jarFile);
                discoveredModules.addAll(result.passed());
                issues.addAll(result.issues().all());
            } catch (ModuleDiscoveryException e) {
                LOGGER.warn(
                        "Skipping jar module file: {} - {}",
                        jarFile.getName(),
                        e.getMessage()
                );
            }
        }
        return ModuleRegistrationStageResult.of(discoveredModules, new ModuleRegistrationIssues(issues));
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

    private ModuleRegistrationStageResult<ModuleCandidate> discoverModulesInJar(File jarFile) {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        try {
            try (JarFile jar = new JarFile(jarFile)) {
                List<JarEntry> classEntries = new ArrayList<>();
                jar.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().endsWith(".class"))
                        .forEach(classEntries::add);
                classEntries.sort(Comparator.comparing(JarEntry::getName));

                for (JarEntry entry : classEntries) {
                    readModuleMetadata(jarFile, jar, entry)
                            .map(metadata -> discoverModuleClass(jarFile, metadata))
                            .ifPresent(result -> {
                                discoveredModules.addAll(result.passed());
                                issues.addAll(result.issues().all());
                            });
                }
            }
        } catch (Exception e) {
            throw new ModuleDiscoveryException("Failed to discover modules from jar: " + jarFile.getAbsolutePath(), e);
        }
        return ModuleRegistrationStageResult.of(discoveredModules, new ModuleRegistrationIssues(issues));
    }

    private java.util.Optional<JarModuleMetadata> readModuleMetadata(File jarFile, JarFile jar, JarEntry entry) {
        try (InputStream input = jar.getInputStream(entry)) {
            return metadataReader.read(input);
        } catch (Throwable e) {
            LOGGER.warn(
                    "Failed to read class metadata in jar: {} -> {} - {}",
                    jarFile.getName(),
                    entry.getName(),
                    describe(e)
            );
            return java.util.Optional.empty();
        }
    }

    private void addJarToClasspath(File jarFile) {
        try {
            File normalizedJarFile = jarFile.getCanonicalFile();
            if (!loadedJarFiles.add(normalizedJarFile)) {
                return;
            }
            URL jarUrl = normalizedJarFile.toURI().toURL();
            BlueClassLoaderSupport.addUrl(classLoader, jarUrl);
        } catch (BlueMatrixLoaderException e) {
            throw new ModuleDiscoveryException("Failed to add jar module to classpath: " + jarFile.getAbsolutePath(), e);
        } catch (Exception e) {
            throw new ModuleDiscoveryException("Failed to prepare jar module classpath: " + jarFile.getAbsolutePath(), e);
        }
    }

    private ModuleRegistrationStageResult<ModuleCandidate> discoverModuleClass(File jarFile, JarModuleMetadata metadata) {
        String className = metadata.className();
        try {
            ModuleRuntimeLibraryLoadResult libraryResult = loadRuntimeLibraries(metadata);
            if (libraryResult.failed()) {
                LOGGER.warn(
                        "Skipping module in jar: {} -> {} ({}) - failed to load runtime libraries: {}",
                        jarFile.getName(),
                        metadata.className(),
                        metadata.id(),
                        libraryResult.failureSummary()
                );
                ModuleRegistrationIssue issue = new RuntimeLibraryLoadFailedIssue(
                        metadata.id(),
                        metadata.name(),
                        metadata.className(),
                        libraryResult,
                        "Failed to load runtime libraries: " + libraryResult.failureSummary()
                );
                return ModuleRegistrationStageResult.of(
                        java.util.Collections.emptyList(),
                        new ModuleRegistrationIssues(java.util.Collections.singletonList(issue))
                );
            }
            addJarToClasspath(jarFile);
            Class<?> clazz = classLoader.loadClass(className);
            if (!isModuleClass(clazz)) {
                return ModuleRegistrationStageResult.empty();
            }

            @SuppressWarnings("unchecked")
            Class<? extends Module> moduleClass = (Class<? extends Module>) clazz;
            ModuleInfo info = moduleClass.getAnnotation(ModuleInfo.class);
            ModuleCandidate candidate = new ModuleCandidate(moduleClass, info);
            return ModuleRegistrationStageResult.of(java.util.Collections.singletonList(candidate));
        } catch (Throwable e) {
            LOGGER.warn(
                    "Failed to inspect class in jar: {} -> {} - {}",
                    jarFile.getName(),
                    className,
                    describe(e)
            );
            return ModuleRegistrationStageResult.empty();
        }
    }

    private ModuleRuntimeLibraryLoadResult loadRuntimeLibraries(JarModuleMetadata metadata) {
        if (runtimeLibraryLoader == null) {
            return ModuleRuntimeLibraryLoadResult.success(metadata.id());
        }
        return runtimeLibraryLoader.load(metadata.id(), metadata.repositories(), metadata.libraries());
    }

    private String describe(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }

    private boolean isModuleClass(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        return Module.class.isAssignableFrom(clazz)
                && clazz.isAnnotationPresent(ModuleInfo.class)
                && !clazz.isInterface()
                && !Modifier.isAbstract(modifiers);
    }

}
