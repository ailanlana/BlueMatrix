package io.fntlv.bluematrix.lang.core.loader.declaration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlueLangDeclaration {
    private final Field field;
    private final String key;
    private final boolean sectionStorage;
    private final List<BlueLangDeclaredText> texts;

    public BlueLangDeclaration(Field field, String key, boolean sectionStorage, List<BlueLangDeclaredText> texts) {
        this.field = field;
        this.key = key;
        this.sectionStorage = sectionStorage;
        this.texts = Collections.unmodifiableList(new ArrayList<>(texts));
    }

    public Field field() {
        return field;
    }

    public String key() {
        return key;
    }

    public boolean sectionStorage() {
        return sectionStorage;
    }

    public List<BlueLangDeclaredText> texts() {
        return texts;
    }
}
