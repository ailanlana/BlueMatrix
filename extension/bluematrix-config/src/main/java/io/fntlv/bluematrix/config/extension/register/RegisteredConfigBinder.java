package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.util.List;

class RegisteredConfigBinder {
    private final RegisteredConfigDefinitionScanner definitionScanner;
    private final RegisteredConfigBindingOperation bindingOperation;

    RegisteredConfigBinder() {
        this(new RegisteredConfigDefinitionScanner(), new RegisteredConfigBindingOperation());
    }

    RegisteredConfigBinder(RegisteredConfigDefinitionScanner definitionScanner,
                           RegisteredConfigBindingOperation bindingOperation) {
        if (definitionScanner == null) {
            throw new IllegalArgumentException("definitionScanner cannot be null");
        }
        if (bindingOperation == null) {
            throw new IllegalArgumentException("bindingOperation cannot be null");
        }
        this.definitionScanner = definitionScanner;
        this.bindingOperation = bindingOperation;
    }

    RegisteredConfig create(Object configInstance,
                            Class<?> configClass,
                            ConfigFile file,
                            String category) {
        List<RegisteredConfigField> registeredFields = definitionScanner.scan(configClass, category);
        return new RegisteredConfig(configClass, configInstance, file, registeredFields);
    }

    void load(RegisteredConfig config) {
        bindingOperation.load(config);
    }

    void save(RegisteredConfig config) {
        bindingOperation.save(config);
    }
}
