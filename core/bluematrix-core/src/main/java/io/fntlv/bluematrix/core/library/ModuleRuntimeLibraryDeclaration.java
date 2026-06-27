package io.fntlv.bluematrix.core.library;

final class ModuleRuntimeLibraryDeclaration {
    private final String moduleId;
    private final RuntimeLibraryDeclaration declaration;

    ModuleRuntimeLibraryDeclaration(String moduleId, RuntimeLibraryDeclaration declaration) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        this.moduleId = moduleId.trim();
        this.declaration = declaration;
    }

    String moduleId() {
        return moduleId;
    }

    RuntimeLibraryDeclaration declaration() {
        return declaration;
    }
}
