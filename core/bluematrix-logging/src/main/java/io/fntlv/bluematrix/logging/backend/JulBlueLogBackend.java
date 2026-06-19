package io.fntlv.bluematrix.logging.backend;

import io.fntlv.bluematrix.logging.BlueLogLevel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JulBlueLogBackend implements BlueLogBackend {
    private final Logger logger;

    public JulBlueLogBackend(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger cannot be null");
        }
        this.logger = logger;
    }

    @Override
    public boolean isEnabled(BlueLogLevel level) {
        return logger.isLoggable(toJulLevel(level));
    }

    @Override
    public void log(BlueLogLevel level, String message) {
        logger.log(toJulLevel(level), message);
    }

    @Override
    public void log(BlueLogLevel level, String message, Throwable throwable) {
        logger.log(toJulLevel(level), message, throwable);
    }

    private Level toJulLevel(BlueLogLevel level) {
        switch (level) {
            case DEBUG:
                return Level.FINE;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARNING;
            case ERROR:
                return Level.SEVERE;
            default:
                return Level.INFO;
        }
    }
}
