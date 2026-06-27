package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

final class RegisteredConfigBindingOperation {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(RegisteredConfigBindingOperation.class);

    private final RegisteredConfigValueAdapter valueAdapter;

    RegisteredConfigBindingOperation() {
        this(new RegisteredConfigValueAdapter());
    }

    RegisteredConfigBindingOperation(RegisteredConfigValueAdapter valueAdapter) {
        if (valueAdapter == null) {
            throw new IllegalArgumentException("valueAdapter cannot be null");
        }
        this.valueAdapter = valueAdapter;
    }

    void load(RegisteredConfig config) {
        for (RegisteredConfigField field : config.fields()) {
            loadField(config.file(), config, field);
        }
    }

    void save(RegisteredConfig config) {
        for (RegisteredConfigField field : config.fields()) {
            saveField(config.file(), config, field);
        }
    }

    private void loadField(ConfigFile file, RegisteredConfig config, RegisteredConfigField field) {
        try {
            boolean existed = file.contains(field.path());
            file.setDefault(field.path(), field.defaultValue(), field.comment());
            Object finalValue = valueAdapter.read(file, field);
            field.field().set(config.instance(), finalValue);
            if (!existed) {
                file.set(field.path(), valueAdapter.toStoredValue(finalValue));
            }

            LOGGER.debug("Injected configuration [{}] => {}.{} (Type: {})",
                    field.path(),
                    config.type().getSimpleName(),
                    field.field().getName(),
                    field.field().getType().getSimpleName());
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration injection failed: %s.%s (%s)",
                    config.type().getSimpleName(),
                    field.field().getName(),
                    e.getMessage());

            throw new ConfigInjectionException(errorMsg, e);
        }
    }

    private void saveField(ConfigFile file, RegisteredConfig config, RegisteredConfigField field) {
        try {
            file.set(field.path(), valueAdapter.toStoredValue(field.field().get(config.instance())));
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration save failed: %s.%s (%s)",
                    config.type().getSimpleName(),
                    field.field().getName(),
                    e.getMessage());

            throw new ConfigInjectionException(errorMsg, e);
        }
    }
}
