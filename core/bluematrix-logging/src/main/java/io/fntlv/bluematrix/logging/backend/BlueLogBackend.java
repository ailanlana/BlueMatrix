package io.fntlv.bluematrix.logging.backend;

import io.fntlv.bluematrix.logging.BlueLogLevel;

public interface BlueLogBackend {

    boolean isEnabled(BlueLogLevel level);

    void log(BlueLogLevel level, String message);

    void log(BlueLogLevel level, String message, Throwable throwable);
}
