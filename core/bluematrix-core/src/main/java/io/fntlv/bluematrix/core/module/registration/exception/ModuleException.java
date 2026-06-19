package io.fntlv.bluematrix.core.module.registration.exception;

import io.fntlv.bluematrix.core.BlueMatrixException;

/**
 * Represents errors related to module loading, configuration or dependency resolution.
 */
public class ModuleException extends BlueMatrixException {

    public ModuleException() {
        super();
    }

    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleException(Throwable cause) {
        super(cause);
    }

    public ModuleException(String format, Object... args) {
        super(String.format(format, args));
    }
}
