package io.fntlv.bluematrix.core.library.declaration;

import io.fntlv.bluematrix.core.library.runtime.RuntimeLibraryNames;

import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.util.Objects;

public final class RuntimeLibraryDeclaration {
    private final BlueLibrary library;
    private final String presenceClass;

    public RuntimeLibraryDeclaration(BlueLibrary library, String presenceClass) {
        this.library = Objects.requireNonNull(library, "library");
        this.presenceClass = RuntimeLibraryNames.normalize(presenceClass);
    }

    public BlueLibrary library() {
        return library;
    }

    public String presenceClass() {
        return presenceClass;
    }
}
