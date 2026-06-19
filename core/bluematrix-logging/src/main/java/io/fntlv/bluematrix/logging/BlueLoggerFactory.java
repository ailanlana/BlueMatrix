package io.fntlv.bluematrix.logging;

import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import io.fntlv.bluematrix.logging.backend.BlueLogBackendProvider;
import io.fntlv.bluematrix.logging.backend.Slf4jBlueLogBackend;
import io.fntlv.bluematrix.logging.support.DefaultBlueLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlueLoggerFactory {
    private static final String DEFAULT_LOGGER_NAME = "BlueMatrix";
    private static final Map<String, DefaultBlueLogger> LOGGERS = new ConcurrentHashMap<>();
    private static volatile BlueLogBackendProvider backendProvider = Slf4jBlueLogBackend::new;
    private static volatile boolean debugEnabled;

    private BlueLoggerFactory() {
    }

    public static BlueLogger getLogger() {
        return getLogger(DEFAULT_LOGGER_NAME);
    }

    public static BlueLogger getLogger(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return getLogger(type.getName());
    }

    public static BlueLogger getLogger(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        return LOGGERS.computeIfAbsent(name, BlueLoggerFactory::createLogger);
    }

    public static void setBackendProvider(BlueLogBackendProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider cannot be null");
        }
        backendProvider = provider;
        LOGGERS.replaceAll((name, logger) -> {
            logger.setBackend(provider.getBackend(name));
            return logger;
        });
    }

    public static BlueLogBackendProvider getBackendProvider() {
        return backendProvider;
    }

    public static void setBackend(BlueLogBackend backend) {
        defaultLogger().setBackend(backend);
    }

    public static BlueLogBackend getBackend() {
        return defaultLogger().getBackend();
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        LOGGERS.values().forEach(logger -> logger.setDebugEnabled(enabled));
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    private static DefaultBlueLogger createLogger(String name) {
        DefaultBlueLogger logger = new DefaultBlueLogger(name, backendProvider.getBackend(name));
        logger.setDebugEnabled(debugEnabled);
        return logger;
    }

    private static DefaultBlueLogger defaultLogger() {
        return (DefaultBlueLogger) getLogger(DEFAULT_LOGGER_NAME);
    }
}
