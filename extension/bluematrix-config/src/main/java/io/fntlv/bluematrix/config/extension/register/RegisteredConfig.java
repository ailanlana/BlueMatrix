package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.util.Collections;
import java.util.List;

public class RegisteredConfig {
    private final Class<?> type;
    private final Object instance;
    private final ConfigFile file;
    private final List<RegisteredConfigField> fields;

    public RegisteredConfig(Class<?> type, Object instance, List<RegisteredConfigField> fields) {
        this(type, instance, null, fields);
    }

    public RegisteredConfig(Class<?> type, Object instance, ConfigFile file, List<RegisteredConfigField> fields) {
        this.type = type;
        this.instance = instance;
        this.file = file;
        this.fields = Collections.unmodifiableList(fields);
    }

    public Class<?> type() {
        return type;
    }

    public Object instance() {
        return instance;
    }

    public ConfigFile file() {
        return file;
    }

    public List<RegisteredConfigField> fields() {
        return fields;
    }
}
