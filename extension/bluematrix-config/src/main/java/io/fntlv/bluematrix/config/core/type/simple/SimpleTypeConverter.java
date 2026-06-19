package io.fntlv.bluematrix.config.core.type.simple;

public interface SimpleTypeConverter {

    boolean supports(Class<?> targetType);

    Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context);
}
