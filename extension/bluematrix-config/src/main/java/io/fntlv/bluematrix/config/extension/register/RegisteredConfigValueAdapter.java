package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class RegisteredConfigValueAdapter {

    Object read(ConfigFile file, RegisteredConfigField field) {
        if (field.list()) {
            return file.getList(field.path(), field.listElementType());
        }
        return file.get(field.path(), field.field().getType());
    }

    Object toStoredValue(Object value) {
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Collection<?>) {
            return collectionToStoredValue((Collection<?>) value);
        }
        return value;
    }

    private Object collectionToStoredValue(Collection<?> value) {
        List<Object> stored = new ArrayList<>();
        boolean containsEnum = false;
        for (Object item : value) {
            if (item instanceof Enum<?>) {
                containsEnum = true;
                stored.add(((Enum<?>) item).name());
            } else {
                stored.add(item);
            }
        }
        return containsEnum ? stored : value;
    }
}
