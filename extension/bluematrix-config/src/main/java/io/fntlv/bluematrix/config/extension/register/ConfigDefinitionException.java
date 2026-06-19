package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.ConfigException;

public class ConfigDefinitionException extends ConfigException {

    public ConfigDefinitionException(String message) {
        super(message);
    }

    public ConfigDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
