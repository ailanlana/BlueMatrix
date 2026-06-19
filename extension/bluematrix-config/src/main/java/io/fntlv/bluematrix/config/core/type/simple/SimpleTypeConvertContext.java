package io.fntlv.bluematrix.config.core.type.simple;

import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public class SimpleTypeConvertContext {

    private final String path;
    private final Field field;

    private SimpleTypeConvertContext(String path, Field field) {
        this.path = path;
        this.field = field;
    }

    public static SimpleTypeConvertContext of(String path) {
        return new SimpleTypeConvertContext(path, null);
    }

    public static SimpleTypeConvertContext of(String path, Field field) {
        return new SimpleTypeConvertContext(path, field);
    }

    public String describe() {
        if (field == null) {
            return "path " + path;
        }
        return "path " + path + " (" + field.getDeclaringClass().getSimpleName() + "." + field.getName() + ")";
    }
}
