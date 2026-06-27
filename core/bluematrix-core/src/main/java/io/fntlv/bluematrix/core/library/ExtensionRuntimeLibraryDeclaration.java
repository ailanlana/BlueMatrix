package io.fntlv.bluematrix.core.library;

final class ExtensionRuntimeLibraryDeclaration {
    private final String extensionName;
    private final RuntimeLibraryDeclaration declaration;

    ExtensionRuntimeLibraryDeclaration(String extensionName, RuntimeLibraryDeclaration declaration) {
        if (extensionName == null || extensionName.trim().isEmpty()) {
            throw new IllegalArgumentException("extensionName cannot be blank");
        }
        this.extensionName = extensionName.trim();
        this.declaration = declaration;
    }

    String extensionName() {
        return extensionName;
    }

    RuntimeLibraryDeclaration declaration() {
        return declaration;
    }
}
