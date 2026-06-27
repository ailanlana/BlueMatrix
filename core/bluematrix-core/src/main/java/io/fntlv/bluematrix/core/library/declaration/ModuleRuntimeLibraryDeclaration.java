package io.fntlv.bluematrix.core.library.declaration;

public final class ModuleRuntimeLibraryDeclaration {
    private final String moduleId;
    private final RuntimeLibraryDeclaration declaration;

    public ModuleRuntimeLibraryDeclaration(String moduleId, RuntimeLibraryDeclaration declaration) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        this.moduleId = moduleId.trim();
        this.declaration = declaration;
    }

    public String moduleId() {
        return moduleId;
    }

    public RuntimeLibraryDeclaration declaration() {
        return declaration;
    }
}
