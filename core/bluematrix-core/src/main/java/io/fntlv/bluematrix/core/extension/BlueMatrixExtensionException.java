package io.fntlv.bluematrix.core.extension;

import io.fntlv.bluematrix.core.BlueMatrixException;

public class BlueMatrixExtensionException extends BlueMatrixException {
    public BlueMatrixExtensionException(String message) {
        super(message);
    }

    public BlueMatrixExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
