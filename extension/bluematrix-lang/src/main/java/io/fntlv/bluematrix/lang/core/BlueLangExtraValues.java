package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.config.core.Configs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlueLangExtraValues {
    private final Map<String, Object> values;

    public BlueLangExtraValues(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public String getString(String name, String defaultValue) {
        Object value = values.get(name);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public <T extends Enum<T>> T getEnum(String name, Class<T> enumType, T defaultValue) {
        Object value = values.get(name);
        if (value == null) {
            return defaultValue;
        }
        return Configs.simpleTypeConverters().convert(value, enumType, name);
    }

    public List<String> getStringList(String name) {
        Object value = values.get(name);
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List)) {
            return Collections.singletonList(String.valueOf(value));
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(String.valueOf(item));
        }
        return result;
    }
}
