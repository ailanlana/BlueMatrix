package io.fntlv.bluematrix.config.core.type.complex;

import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeLoadException;
import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeSaveException;
import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeHandlerException;
import io.fntlv.bluematrix.config.core.section.ConfigSection;
import io.fntlv.bluematrix.config.core.type.complex.function.ConfigLoadFunction;
import io.fntlv.bluematrix.config.core.type.complex.function.ConfigSaveFunction;
import io.fntlv.bluematrix.config.core.type.complex.function.StringDeserializeFunction;
import io.fntlv.bluematrix.config.core.type.complex.function.StringSerializeFunction;

import java.util.Objects;

public class ComplexTypeHandler<T> {

    private final Class<T> type;
    private ConfigLoadFunction<T> configLoadFunction;
    private ConfigSaveFunction<T> configSaveFunction;
    private StringSerializeFunction<T> stringSerializeFunction;
    private StringDeserializeFunction<T> stringDeserializeFunction;

    ComplexTypeHandler(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public boolean supports(Class<?> requestedType) {
        return type.isAssignableFrom(requestedType);
    }

    public ComplexTypeHandler<T> onConfigLoad(ConfigLoadFunction<T> function) {
        this.configLoadFunction = Objects.requireNonNull(function, "function");
        return this;
    }

    public ComplexTypeHandler<T> onConfigSave(ConfigSaveFunction<T> function) {
        this.configSaveFunction = Objects.requireNonNull(function, "function");
        return this;
    }

    public ComplexTypeHandler<T> onStringSerialize(StringSerializeFunction<T> function) {
        this.stringSerializeFunction = Objects.requireNonNull(function, "function");
        return this;
    }

    public ComplexTypeHandler<T> onStringDeserialize(StringDeserializeFunction<T> function) {
        this.stringDeserializeFunction = Objects.requireNonNull(function, "function");
        return this;
    }

    public boolean canLoad() {
        return configLoadFunction != null || stringDeserializeFunction != null;
    }

    public boolean canSave() {
        return configSaveFunction != null || stringSerializeFunction != null;
    }

    public boolean canDeserializeString() {
        return stringDeserializeFunction != null;
    }

    public boolean canSerializeString() {
        return stringSerializeFunction != null;
    }

    public boolean canSerializeToStringList() {
        return canSerializeString() && canDeserializeString();
    }

    public T load(ConfigSection section, Class<T> requestedType) {
        try {
            if (stringDeserializeFunction != null) {
                String value = section.getString("");
                if (value != null) {
                    return stringDeserializeFunction.deserialize(value, requestedType);
                }
            }
            if (configLoadFunction != null) {
                return configLoadFunction.load(section, requestedType);
            }
        } catch (Exception e) {
            throw new ConfigTypeLoadException("Failed to load config type " + type.getName() + " at path " + section.getPath(), e);
        }
        throw new ConfigTypeHandlerException("No config load function registered for " + type.getName() + " at path " + section.getPath());
    }

    public void save(ConfigSection section, T value) {
        try {
            if (configSaveFunction != null) {
                configSaveFunction.save(section, value);
                return;
            }
            if (stringSerializeFunction != null) {
                section.set("", value == null ? null : stringSerializeFunction.serialize(value));
                return;
            }
        } catch (Exception e) {
            throw new ConfigTypeSaveException("Failed to save config type " + type.getName() + " at path " + section.getPath(), e);
        }
        throw new ConfigTypeHandlerException("No config save function registered for " + type.getName() + " at path " + section.getPath());
    }

    public T deserializeString(String value, Class<T> requestedType) {
        if (stringDeserializeFunction == null) {
            throw new ConfigTypeHandlerException("No string deserialize function registered for " + type.getName());
        }
        try {
            return stringDeserializeFunction.deserialize(value, requestedType);
        } catch (Exception e) {
            throw new ConfigTypeLoadException("Failed to deserialize config type " + type.getName() + " from string value " + value, e);
        }
    }

    public String serializeString(T value) {
        if (stringSerializeFunction == null) {
            throw new ConfigTypeHandlerException("No string serialize function registered for " + type.getName());
        }
        try {
            return stringSerializeFunction.serialize(value);
        } catch (Exception e) {
            throw new ConfigTypeSaveException("Failed to serialize config type " + type.getName() + " to string", e);
        }
    }
}
