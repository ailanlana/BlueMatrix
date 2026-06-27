package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.util.Objects;

final class RuntimeLibraryDeclaration {
    private final BlueLibrary library;
    private final String presenceClass;

    RuntimeLibraryDeclaration(BlueLibrary library, String presenceClass) {
        this.library = Objects.requireNonNull(library, "library");
        this.presenceClass = RuntimeLibraryNames.normalize(presenceClass);
    }

    BlueLibrary library() {
        return library;
    }

    String presenceClass() {
        return presenceClass;
    }
}
