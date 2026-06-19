package io.fntlv.bluematrix.config.extension.register;

import java.lang.reflect.Field;

public class RegisteredConfigField {
    private final Field field;
    private final String path;
    private final Object defaultValue;
    private final String comment;
    private final Class<?> listElementType;

    public RegisteredConfigField(Field field,
                                 String path,
                                 Object defaultValue,
                                 String comment,
                                 Class<?> listElementType) {
        this.field = field;
        this.path = path;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.listElementType = listElementType;
    }

    public Field field() {
        return field;
    }

    public String path() {
        return path;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public String comment() {
        return comment;
    }

    public Class<?> listElementType() {
        return listElementType;
    }

    public boolean list() {
        return listElementType != null;
    }
}
