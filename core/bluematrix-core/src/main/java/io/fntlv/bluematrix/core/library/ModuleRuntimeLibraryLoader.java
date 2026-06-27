package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.module.ModuleInfo;

public final class ModuleRuntimeLibraryLoader {
    private final BlueMatrixLibraryLoader libraryLoader;

    public ModuleRuntimeLibraryLoader(BlueMatrixLibraryLoader libraryLoader) {
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader cannot be null");
        }
        this.libraryLoader = libraryLoader;
    }

    public void load(ModuleInfo moduleInfo) {
        if (moduleInfo == null) {
            throw new IllegalArgumentException("moduleInfo cannot be null");
        }
        load(moduleInfo.id(), moduleInfo.repositories(), moduleInfo.libraries());
    }

    public void load(String moduleId, String[] repositories, String[] libraries) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (libraries == null || libraries.length == 0) {
            return;
        }
        try {
            if (repositories != null) {
                for (String repository : repositories) {
                    libraryLoader.addModuleRepository(moduleId, repository);
                }
            }
            for (String library : libraries) {
                libraryLoader.addModuleLibrary(moduleId, library);
            }
            libraryLoader.loadModuleLibraries(moduleId);
        } catch (RuntimeException e) {
            throw new ModuleRuntimeLibraryException(
                    "Failed to load runtime libraries for module: " + moduleId,
                    e
            );
        }
    }
}
