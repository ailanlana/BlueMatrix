package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.library.declaration.ExtensionRuntimeLibraryDeclaration;
import io.fntlv.bluematrix.core.library.declaration.ModuleRuntimeLibraryDeclaration;
import io.fntlv.bluematrix.core.library.declaration.RuntimeLibraryDeclaration;
import io.fntlv.bluematrix.core.library.runtime.RuntimeLibraryInstaller;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

public final class BlueMatrixLibraryLoader {
    private static final Logger LOGGER = Logger.getLogger(BlueMatrixLibraryLoader.class.getName());
    private static final Set<ClassLoader> CORE_LOADED_CLASS_LOADERS = newSetFromWeakMap();
    private static Downloader downloader = new DefaultDownloader();

    private final ClassLoader classLoader;
    private final RuntimeLibraryInstaller installer;
    private final List<RuntimeLibraryDeclaration> appLibraries = new ArrayList<>();
    private final List<ExtensionRuntimeLibraryDeclaration> extensionLibraries = new ArrayList<>();
    private final List<ModuleRuntimeLibraryDeclaration> moduleLibraries = new ArrayList<>();

    public BlueMatrixLibraryLoader(File dataFolder, ClassLoader classLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        this.classLoader = classLoader;
        this.installer = new RuntimeLibraryInstaller(dataFolder, classLoader);
    }

    public BlueMatrixLibraryLoader addRepository(String repositoryUrl) {
        installer.addRepository(repositoryUrl);
        return this;
    }

    public BlueMatrixLibraryLoader addAppLibrary(String coordinates) {
        return addAppLibrary(BlueLibraryFactory.of(coordinates));
    }

    public BlueMatrixLibraryLoader addAppLibrary(BlueLibrary library) {
        return addAppLibrary(library, null);
    }

    public BlueMatrixLibraryLoader addAppLibrary(String coordinates, String presenceClass) {
        return addAppLibrary(BlueLibraryFactory.of(coordinates), presenceClass);
    }

    public BlueMatrixLibraryLoader addAppLibrary(BlueLibrary library, String presenceClass) {
        appLibraries.add(new RuntimeLibraryDeclaration(Objects.requireNonNull(library, "library"), presenceClass));
        return this;
    }

    public BlueMatrixLibraryLoader addExtensionLibrary(String extensionName, String coordinates) {
        return addExtensionLibrary(extensionName, BlueLibraryFactory.of(coordinates));
    }

    public BlueMatrixLibraryLoader addExtensionLibrary(String extensionName, BlueLibrary library) {
        return addExtensionLibrary(extensionName, library, null);
    }

    public BlueMatrixLibraryLoader addExtensionLibrary(String extensionName, String coordinates, String presenceClass) {
        return addExtensionLibrary(extensionName, BlueLibraryFactory.of(coordinates), presenceClass);
    }

    public BlueMatrixLibraryLoader addExtensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
        extensionLibraries.add(new ExtensionRuntimeLibraryDeclaration(
                extensionName,
                new RuntimeLibraryDeclaration(Objects.requireNonNull(library, "library"), presenceClass)
        ));
        return this;
    }

    public BlueMatrixLibraryLoader addModuleLibrary(String moduleId, String coordinates) {
        return addModuleLibrary(moduleId, BlueLibraryFactory.of(coordinates));
    }

    public BlueMatrixLibraryLoader addModuleLibrary(String moduleId, BlueLibrary library) {
        moduleLibraries.add(new ModuleRuntimeLibraryDeclaration(
                moduleId,
                new RuntimeLibraryDeclaration(Objects.requireNonNull(library, "library"), null)
        ));
        return this;
    }

    public BlueMatrixLibraryLoader addModuleRepository(String moduleId, String repositoryUrl) {
        installer.addModuleRepository(moduleId, repositoryUrl);
        return this;
    }

    public void downloadModuleLibrary(String moduleId, String coordinates) {
        downloadModuleLibrary(moduleId, BlueLibraryFactory.of(coordinates));
    }

    public void downloadModuleLibrary(String moduleId, BlueLibrary library) {
        download(BlueMatrixLibraryScope.MODULE, moduleId, library);
    }

    public void loadModuleLibraries(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        String normalizedModuleId = moduleId.trim();
        for (ModuleRuntimeLibraryDeclaration declaration : moduleLibraries) {
            if (normalizedModuleId.equals(declaration.moduleId())) {
                download(BlueMatrixLibraryScope.MODULE, declaration.moduleId(), declaration.declaration());
            }
        }
    }

    public void load() {
        loadCoreLibraries();
        loadAppLibraries();
        loadExtensionLibraries();
        loadAllModuleLibraries();
    }

    public void loadCoreLibraries() {
        downloadCoreLibraries();
    }

    public void loadAppLibraries() {
        for (RuntimeLibraryDeclaration declaration : appLibraries) {
            download(BlueMatrixLibraryScope.APP, null, declaration);
        }
    }

    public void loadExtensionLibraries() {
        for (ExtensionRuntimeLibraryDeclaration declaration : extensionLibraries) {
            download(BlueMatrixLibraryScope.EXTENSION, declaration.extensionName(), declaration.declaration());
        }
    }

    private void loadAllModuleLibraries() {
        for (ModuleRuntimeLibraryDeclaration declaration : moduleLibraries) {
            download(BlueMatrixLibraryScope.MODULE, declaration.moduleId(), declaration.declaration());
        }
    }

    private void download(BlueMatrixLibraryScope scope, String qualifier, BlueLibrary library) {
        download(scope, qualifier, new RuntimeLibraryDeclaration(library, null));
    }

    private void download(BlueMatrixLibraryScope scope, String qualifier, RuntimeLibraryDeclaration declaration) {
        installer.download(this, downloader, scope, qualifier, declaration);
    }

    private void downloadAll(BlueMatrixLibraryScope scope, String qualifier, Collection<BlueLibrary> libraries) {
        Objects.requireNonNull(libraries, "libraries");
        for (BlueLibrary library : libraries) {
            download(scope, qualifier, library);
        }
    }

    private void downloadCoreLibraries() {
        synchronized (CORE_LOADED_CLASS_LOADERS) {
            if (CORE_LOADED_CLASS_LOADERS.contains(classLoader)) {
                return;
            }
            loadMissingCoreLibraries();
            CORE_LOADED_CLASS_LOADERS.add(classLoader);
        }
    }

    private void loadMissingCoreLibraries() {
        boolean coreMissing = !installer.isPresent("org.reflections.Reflections")
                || !installer.isPresent("javassist.bytecode.ClassFile");
        boolean loggingMissing = !installer.isPresent("org.slf4j.LoggerFactory")
                || !installer.isPresent("ch.qos.logback.classic.Logger");

        if (!coreMissing && !loggingMissing) {
            return;
        }

        if (coreMissing && loggingMissing) {
            LOGGER.info("Loading BlueMatrix framework dependencies into dataFolder/libs/core");
            downloadAll(BlueMatrixLibraryScope.CORE, null, BlueMatrixLibraries.framework());
            return;
        }
        if (coreMissing) {
            LOGGER.info("Loading BlueMatrix core dependencies into dataFolder/libs/core");
            downloadAll(BlueMatrixLibraryScope.CORE, null, BlueMatrixLibraries.core());
        }
        if (loggingMissing) {
            LOGGER.info("Loading BlueMatrix logging dependencies into dataFolder/libs/core");
            downloadAll(BlueMatrixLibraryScope.CORE, null, BlueMatrixLibraries.logging());
        }
    }

    public static void downloaderForTesting(Downloader downloader) {
        synchronized (BlueMatrixLibraryLoader.class) {
            BlueMatrixLibraryLoader.downloader = downloader == null ? new DefaultDownloader() : downloader;
            CORE_LOADED_CLASS_LOADERS.clear();
        }
    }

    public interface Downloader {
        void download(
                BlueMatrixLibraryLoader loader,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        );
    }

    private static final class DefaultDownloader implements Downloader {
        @Override
        public void download(
                BlueMatrixLibraryLoader loader,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        ) {
            loader.install(scope, qualifier, library);
        }
    }

    private void install(BlueMatrixLibraryScope scope, String qualifier, BlueLibrary library) {
        installer.loadLibrary(scope, qualifier, library);
    }

    List<String> repositoriesForTesting(BlueMatrixLibraryScope scope, String qualifier) {
        return installer.repositoriesForTesting(scope, qualifier);
    }

    private static Set<ClassLoader> newSetFromWeakMap() {
        return java.util.Collections.newSetFromMap(new WeakHashMap<>());
    }
}
