package io.fntlv.bluematrix.config.core.type.simple.converters;

import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConvertContext;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverter;

public class StringSimpleTypeConverter implements SimpleTypeConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == String.class;
    }

    @Override
    public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        return String.valueOf(value);
    }
}
