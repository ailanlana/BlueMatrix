package io.fntlv.bluematrix.core.module.registration.library;

import lombok.Getter;

@Getter
public final class ModuleRuntimeLibraryFailure {
    private final String library;
    private final Throwable cause;

    public ModuleRuntimeLibraryFailure(String library, Throwable cause) {
        this.library = library;
        this.cause = cause;
    }

    public String library() {
        return library;
    }

    public Throwable cause() {
        return cause;
    }
}
