package io.fntlv.bluematrix.config.core.type.exception;

import io.fntlv.bluematrix.config.core.ConfigException;

public class ConfigValueConvertException extends ConfigException {

    public ConfigValueConvertException(String message) {
        super(message);
    }

    public ConfigValueConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
