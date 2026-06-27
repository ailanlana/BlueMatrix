package io.fntlv.bluematrix.core.library;

public class ModuleRuntimeLibraryException extends RuntimeException {
    public ModuleRuntimeLibraryException(String message) {
        super(message);
    }

    public ModuleRuntimeLibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
