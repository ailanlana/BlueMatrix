package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.BlueLibraryManager;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

public final class BlueMatrixLibraryLoader {
    private static final Logger LOGGER = Logger.getLogger(BlueMatrixLibraryLoader.class.getName());
    private static final Set<ClassLoader> CORE_LOADED_CLASS_LOADERS = newSetFromWeakMap();
    private static Downloader downloader = new DefaultDownloader();

    private final File dataFolder;
    private final ClassLoader classLoader;
    private final List<String> repositories = new ArrayList<>();
    private final List<LibraryDeclaration> appLibraries = new ArrayList<>();
    private final List<ExtensionLibraryDeclaration> extensionLibraries = new ArrayList<>();
    private final List<ModuleLibraryDeclaration> moduleLibraries = new ArrayList<>();
    private final List<ModuleRepositoryDeclaration> moduleRepositories = new ArrayList<>();
    private final Map<ManagerKey, BlueLibraryManager> managers = new HashMap<>();
    private final Set<LibraryKey> loadedLibraries = new HashSet<>();

    public BlueMatrixLibraryLoader(File dataFolder, ClassLoader classLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
    }

    public BlueMatrixLibraryLoader addRepository(String repositoryUrl) {
        String normalizedRepository = normalizeRepository(repositoryUrl);
        if (repositories.contains(normalizedRepository)) {
            return this;
        }
        repositories.add(normalizedRepository);
        for (BlueLibraryManager manager : managers.values()) {
            manager.addRepository(normalizedRepository);
        }
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
        appLibraries.add(new LibraryDeclaration(Objects.requireNonNull(library, "library"), presenceClass));
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
        extensionLibraries.add(new ExtensionLibraryDeclaration(extensionName, Objects.requireNonNull(library, "library"), presenceClass));
        return this;
    }

    public BlueMatrixLibraryLoader addModuleLibrary(String moduleId, String coordinates) {
        return addModuleLibrary(moduleId, BlueLibraryFactory.of(coordinates));
    }

    public BlueMatrixLibraryLoader addModuleLibrary(String moduleId, BlueLibrary library) {
        moduleLibraries.add(new ModuleLibraryDeclaration(moduleId, Objects.requireNonNull(library, "library")));
        return this;
    }

    public BlueMatrixLibraryLoader addModuleRepository(String moduleId, String repositoryUrl) {
        ModuleRepositoryDeclaration declaration = new ModuleRepositoryDeclaration(moduleId, normalizeRepository(repositoryUrl));
        if (moduleRepositories.stream().anyMatch(declaration::equals)) {
            return this;
        }
        moduleRepositories.add(declaration);

        ManagerKey managerKey = new ManagerKey(BlueMatrixLibraryScope.MODULE, declaration.moduleId);
        BlueLibraryManager manager = managers.get(managerKey);
        if (manager != null) {
            manager.addRepository(declaration.repositoryUrl);
        }
        return this;
    }

    public void loadModuleLibraries(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        String normalizedModuleId = moduleId.trim();
        for (ModuleLibraryDeclaration declaration : moduleLibraries) {
            if (normalizedModuleId.equals(declaration.moduleId)) {
                download(BlueMatrixLibraryScope.MODULE, declaration.moduleId, declaration.library);
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
        for (LibraryDeclaration declaration : appLibraries) {
            download(BlueMatrixLibraryScope.APP, null, declaration);
        }
    }

    public void loadExtensionLibraries() {
        for (ExtensionLibraryDeclaration declaration : extensionLibraries) {
            download(
                    BlueMatrixLibraryScope.EXTENSION,
                    declaration.extensionName,
                    new LibraryDeclaration(declaration.library, declaration.presenceClass)
            );
        }
    }

    private void loadAllModuleLibraries() {
        for (ModuleLibraryDeclaration declaration : moduleLibraries) {
            download(BlueMatrixLibraryScope.MODULE, declaration.moduleId, declaration.library);
        }
    }

    private void download(BlueMatrixLibraryScope scope, String qualifier, BlueLibrary library) {
        download(scope, qualifier, new LibraryDeclaration(library, null));
    }

    private void download(BlueMatrixLibraryScope scope, String qualifier, LibraryDeclaration declaration) {
        BlueLibrary library = declaration.library;
        validate(scope, qualifier, library);
        if (isPresent(declaration.presenceClass)) {
            return;
        }
        LibraryKey libraryKey = new LibraryKey(scope, qualifier, library.toString());
        if (!loadedLibraries.add(libraryKey)) {
            return;
        }
        try {
            downloader.download(this, dataFolder, classLoader, scope, qualifier, library);
        } catch (RuntimeException | Error e) {
            loadedLibraries.remove(libraryKey);
            throw e;
        }
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
        boolean coreMissing = !isPresent("org.reflections.Reflections");
        boolean loggingMissing = !isPresent("org.slf4j.LoggerFactory")
                || !isPresent("ch.qos.logback.classic.Logger");

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

    private void validate(BlueMatrixLibraryScope scope, String qualifier, BlueLibrary library) {
        if (scope == null) {
            throw new IllegalArgumentException("scope cannot be null");
        }
        if ((scope == BlueMatrixLibraryScope.EXTENSION || scope == BlueMatrixLibraryScope.MODULE)
                && (qualifier == null || qualifier.trim().isEmpty())) {
            throw new IllegalArgumentException("qualifier cannot be blank");
        }
        if (library == null) {
            throw new IllegalArgumentException("library cannot be null");
        }
    }

    private boolean isPresent(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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
            BlueLibraryManager manager = loader.manager(scope, qualifier);
            manager.loadLibrary(library);
        }
    }

    private BlueLibraryManager manager(BlueMatrixLibraryScope scope, String qualifier) {
        ManagerKey managerKey = new ManagerKey(scope, qualifier);
        return managers.computeIfAbsent(managerKey, ignored -> {
            BlueLibraryManager manager = new BlueLibraryManager(
                    scope.managerName(qualifier),
                    scope.rootFolder(dataFolder),
                    scope.libsFolderName(qualifier),
                    classLoader
            );
            manager.addMavenCentral();
            for (String repository : repositories) {
                manager.addRepository(repository);
            }
            if (scope == BlueMatrixLibraryScope.MODULE) {
                for (ModuleRepositoryDeclaration declaration : moduleRepositories) {
                    if (declaration.moduleId.equals(normalize(qualifier))) {
                        manager.addRepository(declaration.repositoryUrl);
                    }
                }
            }
            return manager;
        });
    }

    List<String> repositoriesForTesting(BlueMatrixLibraryScope scope, String qualifier) {
        List<String> result = new ArrayList<>(repositories);
        if (scope == BlueMatrixLibraryScope.MODULE) {
            String normalizedQualifier = normalize(qualifier);
            for (ModuleRepositoryDeclaration declaration : moduleRepositories) {
                if (declaration.moduleId.equals(normalizedQualifier)) {
                    result.add(declaration.repositoryUrl);
                }
            }
        }
        return result;
    }

    private static final class ManagerKey {
        private final BlueMatrixLibraryScope scope;
        private final String qualifier;

        private ManagerKey(BlueMatrixLibraryScope scope, String qualifier) {
            this.scope = scope;
            this.qualifier = normalize(qualifier);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ManagerKey)) {
                return false;
            }
            ManagerKey that = (ManagerKey) o;
            return scope == that.scope && Objects.equals(qualifier, that.qualifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope, qualifier);
        }
    }

    private static final class LibraryKey {
        private final BlueMatrixLibraryScope scope;
        private final String qualifier;
        private final String coordinate;

        private LibraryKey(BlueMatrixLibraryScope scope, String qualifier, String coordinate) {
            this.scope = scope;
            this.qualifier = normalize(qualifier);
            this.coordinate = coordinate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LibraryKey)) {
                return false;
            }
            LibraryKey that = (LibraryKey) o;
            return scope == that.scope
                    && Objects.equals(qualifier, that.qualifier)
                    && Objects.equals(coordinate, that.coordinate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope, qualifier, coordinate);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeRepository(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("repositoryUrl cannot be blank");
        }
        return repositoryUrl.trim();
    }

    private static Set<ClassLoader> newSetFromWeakMap() {
        return java.util.Collections.newSetFromMap(new WeakHashMap<>());
    }

    private static final class ExtensionLibraryDeclaration {
        private final String extensionName;
        private final BlueLibrary library;
        private final String presenceClass;

        private ExtensionLibraryDeclaration(String extensionName, BlueLibrary library, String presenceClass) {
            if (extensionName == null || extensionName.trim().isEmpty()) {
                throw new IllegalArgumentException("extensionName cannot be blank");
            }
            this.extensionName = extensionName.trim();
            this.library = library;
            this.presenceClass = normalize(presenceClass);
        }
    }

    private static final class LibraryDeclaration {
        private final BlueLibrary library;
        private final String presenceClass;

        private LibraryDeclaration(BlueLibrary library, String presenceClass) {
            this.library = library;
            this.presenceClass = normalize(presenceClass);
        }
    }

    private static final class ModuleLibraryDeclaration {
        private final String moduleId;
        private final BlueLibrary library;

        private ModuleLibraryDeclaration(String moduleId, BlueLibrary library) {
            if (moduleId == null || moduleId.trim().isEmpty()) {
                throw new IllegalArgumentException("moduleId cannot be blank");
            }
            this.moduleId = moduleId.trim();
            this.library = library;
        }
    }

    private static final class ModuleRepositoryDeclaration {
        private final String moduleId;
        private final String repositoryUrl;

        private ModuleRepositoryDeclaration(String moduleId, String repositoryUrl) {
            if (moduleId == null || moduleId.trim().isEmpty()) {
                throw new IllegalArgumentException("moduleId cannot be blank");
            }
            this.moduleId = moduleId.trim();
            this.repositoryUrl = repositoryUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModuleRepositoryDeclaration)) {
                return false;
            }
            ModuleRepositoryDeclaration that = (ModuleRepositoryDeclaration) o;
            return Objects.equals(moduleId, that.moduleId)
                    && Objects.equals(repositoryUrl, that.repositoryUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleId, repositoryUrl);
        }
    }
}
