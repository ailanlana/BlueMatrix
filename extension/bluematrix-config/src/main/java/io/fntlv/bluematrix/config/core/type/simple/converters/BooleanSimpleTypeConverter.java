package io.fntlv.bluematrix.config.core.type.simple.converters;

import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConvertContext;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverter;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;

public class BooleanSimpleTypeConverter implements SimpleTypeConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == boolean.class || targetType == Boolean.class;
    }

    @Override
    public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        if (value instanceof Boolean) {
            return value;
        }

        String stringValue = String.valueOf(value);
        if (stringValue.equalsIgnoreCase("true") || stringValue.equals("1") || stringValue.equalsIgnoreCase("yes")) {
            return true;
        }
        if (stringValue.equalsIgnoreCase("false") || stringValue.equals("0") || stringValue.equalsIgnoreCase("no")) {
            return false;
        }
        throw new ConfigValueConvertException("Invalid boolean value at " + context.describe() + ": " + value);
    }
}
