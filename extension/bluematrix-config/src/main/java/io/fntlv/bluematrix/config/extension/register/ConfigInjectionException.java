package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.ConfigException;

public class ConfigInjectionException extends ConfigException {

    public ConfigInjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
