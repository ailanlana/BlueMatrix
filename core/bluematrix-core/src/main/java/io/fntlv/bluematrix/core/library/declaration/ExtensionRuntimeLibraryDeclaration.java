package io.fntlv.bluematrix.core.library.declaration;

public final class ExtensionRuntimeLibraryDeclaration {
    private final String extensionName;
    private final RuntimeLibraryDeclaration declaration;

    public ExtensionRuntimeLibraryDeclaration(String extensionName, RuntimeLibraryDeclaration declaration) {
        if (extensionName == null || extensionName.trim().isEmpty()) {
            throw new IllegalArgumentException("extensionName cannot be blank");
        }
        this.extensionName = extensionName.trim();
        this.declaration = declaration;
    }

    public String extensionName() {
        return extensionName;
    }

    public RuntimeLibraryDeclaration declaration() {
        return declaration;
    }
}
