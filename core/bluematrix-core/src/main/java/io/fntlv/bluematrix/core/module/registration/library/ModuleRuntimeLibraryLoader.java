package io.fntlv.bluematrix.core.module.registration.library;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;

public final class ModuleRuntimeLibraryLoader {
    private final BlueMatrixLibraryLoader libraryLoader;

    public ModuleRuntimeLibraryLoader(BlueMatrixLibraryLoader libraryLoader) {
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader cannot be null");
        }
        this.libraryLoader = libraryLoader;
    }

    public ModuleRuntimeLibraryLoadResult load(ModuleDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        return load(descriptor.id(), descriptor.repositories(), descriptor.libraries());
    }

    public ModuleRuntimeLibraryLoadResult load(String moduleId, String[] repositories, String[] libraries) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        java.util.List<ModuleRuntimeLibraryFailure> failures = new java.util.ArrayList<>();
        try {
            if (repositories != null) {
                for (String repository : repositories) {
                    libraryLoader.addModuleRepository(moduleId, repository);
                }
            }
        } catch (RuntimeException e) {
            failures.add(new ModuleRuntimeLibraryFailure(null, e));
            return ModuleRuntimeLibraryLoadResult.of(moduleId, failures);
        }
        if (libraries == null || libraries.length == 0) {
            return ModuleRuntimeLibraryLoadResult.of(moduleId, failures);
        }
        for (String library : libraries) {
            try {
                libraryLoader.downloadModuleLibrary(moduleId, library);
            } catch (RuntimeException e) {
                failures.add(new ModuleRuntimeLibraryFailure(library, e));
            }
        }
        return ModuleRuntimeLibraryLoadResult.of(moduleId, failures);
    }
}
