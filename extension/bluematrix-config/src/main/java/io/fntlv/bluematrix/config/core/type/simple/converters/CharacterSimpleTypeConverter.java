package io.fntlv.bluematrix.config.core.type.simple.converters;

import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConvertContext;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverter;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;

public class CharacterSimpleTypeConverter implements SimpleTypeConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == char.class || targetType == Character.class;
    }

    @Override
    public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        String stringValue = String.valueOf(value);
        if (stringValue.length() != 1) {
            throw new ConfigValueConvertException("Char conversion requires exact 1 character at " + context.describe());
        }
        return stringValue.charAt(0);
    }
}
