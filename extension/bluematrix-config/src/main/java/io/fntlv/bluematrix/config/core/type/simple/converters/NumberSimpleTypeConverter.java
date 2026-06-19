package io.fntlv.bluematrix.config.core.type.simple.converters;

import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConvertContext;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverter;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;

public class NumberSimpleTypeConverter implements SimpleTypeConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == int.class || targetType == Integer.class
                || targetType == long.class || targetType == Long.class
                || targetType == double.class || targetType == Double.class
                || targetType == float.class || targetType == Float.class
                || targetType == short.class || targetType == Short.class
                || targetType == byte.class || targetType == Byte.class;
    }

    @Override
    public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        try {
            if (value instanceof Number) {
                return convertNumber((Number) value, targetType, context);
            }
            String stringValue = String.valueOf(value);
            if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(stringValue);
            if (targetType == long.class || targetType == Long.class) return Long.parseLong(stringValue);
            if (targetType == double.class || targetType == Double.class) return Double.parseDouble(stringValue);
            if (targetType == float.class || targetType == Float.class) return Float.parseFloat(stringValue);
            if (targetType == short.class || targetType == Short.class) return Short.parseShort(stringValue);
            if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(stringValue);
        } catch (Exception e) {
            throw new ConfigValueConvertException(
                    "Cannot convert config value at " + context.describe() + " to " + targetType.getSimpleName() + ": " + value,
                    e
            );
        }
        throw new ConfigValueConvertException("Unsupported number type at " + context.describe() + ": " + targetType.getName());
    }

    private Object convertNumber(Number number, Class<?> targetType, SimpleTypeConvertContext context) {
        if (targetType == int.class || targetType == Integer.class) return number.intValue();
        if (targetType == long.class || targetType == Long.class) return number.longValue();
        if (targetType == double.class || targetType == Double.class) return number.doubleValue();
        if (targetType == float.class || targetType == Float.class) return number.floatValue();
        if (targetType == short.class || targetType == Short.class) return number.shortValue();
        if (targetType == byte.class || targetType == Byte.class) return number.byteValue();
        throw new ConfigValueConvertException("Unsupported number type at " + context.describe() + ": " + targetType.getName());
    }
}
