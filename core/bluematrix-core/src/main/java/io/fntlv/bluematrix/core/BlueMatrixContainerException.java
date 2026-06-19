package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.BlueMatrixException;

public class BlueMatrixContainerException extends BlueMatrixException {
    public BlueMatrixContainerException(String message) {
        super(message);
    }

    public BlueMatrixContainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
