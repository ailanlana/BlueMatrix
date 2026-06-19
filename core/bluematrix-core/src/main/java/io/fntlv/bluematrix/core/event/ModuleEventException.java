package io.fntlv.bluematrix.core.event;

import io.fntlv.bluematrix.core.BlueMatrixException;

public class ModuleEventException extends BlueMatrixException {
    public ModuleEventException(String message) {
        super(message);
    }

    public ModuleEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
