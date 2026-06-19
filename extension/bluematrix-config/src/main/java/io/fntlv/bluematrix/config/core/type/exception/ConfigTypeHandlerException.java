package io.fntlv.bluematrix.config.core.type.exception;

import io.fntlv.bluematrix.config.core.ConfigException;

public class ConfigTypeHandlerException extends ConfigException {

    public ConfigTypeHandlerException(String message) {
        super(message);
    }

    public ConfigTypeHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
