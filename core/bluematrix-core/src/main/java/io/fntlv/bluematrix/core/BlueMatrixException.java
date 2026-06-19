package io.fntlv.bluematrix.core;

public class BlueMatrixException extends RuntimeException {

    public BlueMatrixException() {
        super();
    }

    public BlueMatrixException(String message) {
        super(message);
    }

    public BlueMatrixException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlueMatrixException(Throwable cause) {
        super(cause);
    }
}
