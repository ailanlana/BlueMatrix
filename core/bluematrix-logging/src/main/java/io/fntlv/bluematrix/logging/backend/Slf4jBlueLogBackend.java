package io.fntlv.bluematrix.logging.backend;

import io.fntlv.bluematrix.logging.BlueLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jBlueLogBackend implements BlueLogBackend {
    private final Logger logger;

    public Slf4jBlueLogBackend(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public boolean isEnabled(BlueLogLevel level) {
        switch (level) {
            case DEBUG:
                return logger.isDebugEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
                return logger.isErrorEnabled();
            default:
                return true;
        }
    }

    @Override
    public void log(BlueLogLevel level, String message) {
        switch (level) {
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }

    @Override
    public void log(BlueLogLevel level, String message, Throwable throwable) {
        switch (level) {
            case DEBUG:
                logger.debug(message, throwable);
                break;
            case INFO:
                logger.info(message, throwable);
                break;
            case WARN:
                logger.warn(message, throwable);
                break;
            case ERROR:
                logger.error(message, throwable);
                break;
        }
    }
}
