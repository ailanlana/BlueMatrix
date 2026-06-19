package io.fntlv.bluematrix.logging.support;

import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class DefaultBlueLogger implements BlueLogger {
    @Getter
    private final String name;
    @Getter
    private BlueLogBackend backend;
    @Getter
    @Setter
    private boolean debugEnabled;

    public DefaultBlueLogger(BlueLogBackend backend) {
        this("BlueMatrix", backend);
    }

    public DefaultBlueLogger(String name, BlueLogBackend backend) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.name = name;
        setBackend(backend);
    }

    public void setBackend(BlueLogBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("backend cannot be null");
        }
        this.backend = backend;
    }

    @Override
    public void debug(String message) {
        if (debugEnabled) {
            log(BlueLogLevel.DEBUG, message, (Throwable) null);
        }
    }

    @Override
    public void debug(String format, Object... args) {
        if (debugEnabled) {
            log(BlueLogLevel.DEBUG, format, args);
        }
    }

    @Override
    public void info(String message) {
        log(BlueLogLevel.INFO, message, (Throwable) null);
    }

    @Override
    public void info(String format, Object... args) {
        log(BlueLogLevel.INFO, format, args);
    }

    @Override
    public void warn(String message) {
        log(BlueLogLevel.WARN, message, (Throwable) null);
    }

    @Override
    public void warn(String format, Object... args) {
        log(BlueLogLevel.WARN, format, args);
    }

    @Override
    public void error(String message) {
        log(BlueLogLevel.ERROR, message, (Throwable) null);
    }

    @Override
    public void error(String format, Object... args) {
        log(BlueLogLevel.ERROR, format, args);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log(BlueLogLevel.ERROR, message, throwable);
    }

    private void log(BlueLogLevel level, String format, Object[] args) {
        if (!backend.isEnabled(level)) {
            return;
        }
        Throwable throwable = extractThrowable(format, args);
        Object[] formatArgs = throwable == null ? args : Arrays.copyOf(args, args.length - 1);
        log(level, formatMessage(format, formatArgs), throwable);
    }

    private void log(BlueLogLevel level, String message, Throwable throwable) {
        if (!backend.isEnabled(level)) {
            return;
        }
        String formatted = prefix(level) + message;
        if (throwable == null) {
            backend.log(level, formatted);
        } else {
            backend.log(level, formatted, throwable);
        }
    }

    private Throwable extractThrowable(String format, Object[] args) {
        if (args == null || args.length == 0 || !(args[args.length - 1] instanceof Throwable)) {
            return null;
        }
        int placeholders = countPlaceholders(format);
        if (placeholders >= args.length) {
            return null;
        }
        return (Throwable) args[args.length - 1];
    }

    private String formatMessage(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder formatted = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);
            if (currentChar == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    formatted.append(args[argIndex++]);
                    i++;
                } else {
                    formatted.append("{}");
                }
            } else {
                formatted.append(currentChar);
            }
        }
        return formatted.toString();
    }

    private int countPlaceholders(String message) {
        int count = 0;
        for (int i = 0; i < message.length() - 1; i++) {
            if (message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                count++;
                i++;
            }
        }
        return count;
    }

    private String prefix(BlueLogLevel level) {
        switch (level) {
            case DEBUG:
                return "[DEBUG] ";
            case INFO:
                return "[INFO] ";
            case WARN:
                return "[WARN] ";
            case ERROR:
                return "[ERROR] ";
            default:
                return "";
        }
    }
}
