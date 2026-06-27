package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.BlueLibraryManager;
import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RuntimeLibraryInstaller {
    private final File dataFolder;
    private final ClassLoader classLoader;
    private final List<String> repositories = new ArrayList<>();
    private final List<ModuleRuntimeRepositoryDeclaration> moduleRepositories = new ArrayList<>();
    private final Map<RuntimeLibraryManagerKey, BlueLibraryManager> managers = new HashMap<>();
    private final Set<RuntimeLibraryKey> loadedLibraries = new HashSet<>();

    RuntimeLibraryInstaller(File dataFolder, ClassLoader classLoader) {
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
    }

    void addRepository(String repositoryUrl) {
        String normalizedRepository = RuntimeLibraryNames.normalizeRepository(repositoryUrl);
        if (repositories.contains(normalizedRepository)) {
            return;
        }
        repositories.add(normalizedRepository);
        for (BlueLibraryManager manager : managers.values()) {
            manager.addRepository(normalizedRepository);
        }
    }

    void addModuleRepository(String moduleId, String repositoryUrl) {
        ModuleRuntimeRepositoryDeclaration declaration =
                new ModuleRuntimeRepositoryDeclaration(moduleId, repositoryUrl);
        if (moduleRepositories.stream().anyMatch(declaration::equals)) {
            return;
        }
        moduleRepositories.add(declaration);

        RuntimeLibraryManagerKey managerKey =
                new RuntimeLibraryManagerKey(BlueMatrixLibraryScope.MODULE, declaration.moduleId());
        BlueLibraryManager manager = managers.get(managerKey);
        if (manager != null) {
            manager.addRepository(declaration.repositoryUrl());
        }
    }

    void download(
            BlueMatrixLibraryLoader loader,
            BlueMatrixLibraryLoader.Downloader downloader,
            BlueMatrixLibraryScope scope,
            String qualifier,
            RuntimeLibraryDeclaration declaration
    ) {
        BlueLibrary library = declaration.library();
        validate(scope, qualifier, library);
        if (isPresent(declaration.presenceClass())) {
            return;
        }

        RuntimeLibraryKey libraryKey = new RuntimeLibraryKey(scope, qualifier, library.toString());
        if (!loadedLibraries.add(libraryKey)) {
            return;
        }

        try {
            downloader.download(loader, dataFolder, classLoader, scope, qualifier, library);
        } catch (RuntimeException | Error e) {
            loadedLibraries.remove(libraryKey);
            throw e;
        }
    }

    boolean isPresent(String className) {
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

    BlueLibraryManager manager(BlueMatrixLibraryScope scope, String qualifier) {
        RuntimeLibraryManagerKey managerKey = new RuntimeLibraryManagerKey(scope, qualifier);
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
                addModuleRepositories(manager, qualifier);
            }
            return manager;
        });
    }

    List<String> repositoriesForTesting(BlueMatrixLibraryScope scope, String qualifier) {
        List<String> result = new ArrayList<>(repositories);
        if (scope == BlueMatrixLibraryScope.MODULE) {
            String normalizedQualifier = RuntimeLibraryNames.normalize(qualifier);
            for (ModuleRuntimeRepositoryDeclaration declaration : moduleRepositories) {
                if (declaration.moduleId().equals(normalizedQualifier)) {
                    result.add(declaration.repositoryUrl());
                }
            }
        }
        return result;
    }

    private void addModuleRepositories(BlueLibraryManager manager, String qualifier) {
        String normalizedQualifier = RuntimeLibraryNames.normalize(qualifier);
        for (ModuleRuntimeRepositoryDeclaration declaration : moduleRepositories) {
            if (declaration.moduleId().equals(normalizedQualifier)) {
                manager.addRepository(declaration.repositoryUrl());
            }
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
}
