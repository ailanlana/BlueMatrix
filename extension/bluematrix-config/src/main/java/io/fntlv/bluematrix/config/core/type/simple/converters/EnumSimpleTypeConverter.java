package io.fntlv.bluematrix.config.core.type.simple.converters;

import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConvertContext;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverter;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumSimpleTypeConverter implements SimpleTypeConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType.isEnum();
    }

    @Override
    public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
        try {
            return Enum.valueOf((Class<Enum>) targetType, String.valueOf(value));
        } catch (Exception e) {
            throw new ConfigValueConvertException(
                    "Cannot convert config value at " + context.describe() + " to " + targetType.getSimpleName() + ": " + value,
                    e
            );
        }
    }
}
