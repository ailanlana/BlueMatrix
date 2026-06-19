package io.fntlv.bluematrix.config.core.type.simple;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;
import io.fntlv.bluematrix.config.core.type.simple.converters.BooleanSimpleTypeConverter;
import io.fntlv.bluematrix.config.core.type.simple.converters.CharacterSimpleTypeConverter;
import io.fntlv.bluematrix.config.core.type.simple.converters.EnumSimpleTypeConverter;
import io.fntlv.bluematrix.config.core.type.simple.converters.NumberSimpleTypeConverter;
import io.fntlv.bluematrix.config.core.type.simple.converters.StringSimpleTypeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SimpleTypeConverterRegistry {

    private final List<SimpleTypeConverter> converters = new ArrayList<>();

    public SimpleTypeConverterRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(new StringSimpleTypeConverter());
        register(new BooleanSimpleTypeConverter());
        register(new NumberSimpleTypeConverter());
        register(new CharacterSimpleTypeConverter());
        register(new EnumSimpleTypeConverter());
    }

    public SimpleTypeConverterRegistry register(SimpleTypeConverter converter) {
        converters.add(Objects.requireNonNull(converter, "converter"));
        return this;
    }

    public List<SimpleTypeConverter> getConverters() {
        return Collections.unmodifiableList(converters);
    }

    public Optional<SimpleTypeConverter> find(Class<?> targetType) {
        for (SimpleTypeConverter converter : converters) {
            if (converter.supports(targetType)) {
                return Optional.of(converter);
            }
        }
        return Optional.empty();
    }

    public <T> T convert(Object value, Class<T> targetType, String path) {
        Object converted = convertRaw(value, targetType, SimpleTypeConvertContext.of(path));
        return cast(targetType, converted);
    }

    public boolean supports(Class<?> targetType) {
        return find(targetType).isPresent();
    }

    public boolean toBoolean(Object value, String path) {
        Boolean converted = convert(value, Boolean.class, path);
        if (converted == null) {
            throw new ConfigValueConvertException("Missing boolean config value at path: " + path);
        }
        return converted;
    }

    public void clear() {
        clearAll();
        registerDefaults();
    }

    public void clearAll() {
        converters.clear();
    }

    private Object convertRaw(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        for (SimpleTypeConverter converter : converters) {
            if (converter.supports(targetType)) {
                try {
                    return converter.convert(value, targetType, context);
                } catch (ConfigValueConvertException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ConfigValueConvertException(
                            "Cannot convert config value at " + context.describe() + " to " + targetType.getSimpleName() + ": " + value,
                            e
                    );
                }
            }
        }

        throw new ConfigValueConvertException(
                "Unsupported config value type at " + context.describe() + ": " + targetType.getName()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Class<T> targetType, Object value) {
        if (targetType.isPrimitive()) {
            return (T) value;
        }
        return targetType.cast(value);
    }
}
