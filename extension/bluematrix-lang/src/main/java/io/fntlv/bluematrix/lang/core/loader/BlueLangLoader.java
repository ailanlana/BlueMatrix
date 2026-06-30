package io.fntlv.bluematrix.lang.core.loader;

import io.fntlv.bluematrix.lang.core.BlueLangData;
import io.fntlv.bluematrix.lang.core.BlueLangExtraValues;
import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.BlueLangTextFactory;
import io.fntlv.bluematrix.lang.core.loader.declaration.BlueLangDeclaration;
import io.fntlv.bluematrix.lang.core.loader.declaration.BlueLangDeclarationScanner;
import io.fntlv.bluematrix.lang.core.loader.declaration.BlueLangDeclaredText;
import io.fntlv.bluematrix.lang.core.loader.pack.BlueLangPack;
import io.fntlv.bluematrix.lang.core.loader.pack.BlueLangStoredText;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlueLangLoader {
    public static final String LANG_FOLDER_NAME = BlueLangPack.LANG_FOLDER_NAME;

    private final Map<Class<?>, BlueLangTextFactory<?>> factories = new LinkedHashMap<>();

    public BlueLangLoader() {
    }

    public <T> BlueLangLoader register(Class<T> type, BlueLangTextFactory<T> factory) {
        factories.put(type, factory);
        return this;
    }

    public List<BlueLangText> load(File rootFolder, String defaultLang, Class<?> langClass) {
        BlueLangDeclarationScanner scanner = new BlueLangDeclarationScanner(factories.keySet());
        BlueLangPack pack = new BlueLangPack(rootFolder);
        List<BlueLangText> loaded = new ArrayList<>();

        for (BlueLangDeclaration declaration : scanner.scan(langClass)) {
            BlueLangText text = loadText(pack, defaultLang, declaration);
            assign(declaration.field(), createValue(declaration.field(), text));
            loaded.add(text);
        }

        pack.saveIfChanged();
        return loaded;
    }

    private BlueLangText loadText(BlueLangPack pack, String defaultLang, BlueLangDeclaration declaration) {
        List<BlueLangData> data = new ArrayList<>();
        BlueLangStoredText seedText = null;
        boolean hasDefaultLang = false;
        boolean sectionStorage = declaration.sectionStorage();

        for (BlueLangDeclaredText declaredText : declaration.texts()) {
            BlueLangStoredText storedText = pack.readOrSave(declaration.key(), declaredText, sectionStorage);
            data.add(toData(storedText));

            if (seedText == null) {
                seedText = storedText;
            }
            if (defaultLang.equals(storedText.lang())) {
                hasDefaultLang = true;
            }
        }

        if (!hasDefaultLang) {
            data.add(toData(pack.readOrSave(
                    declaration.key(),
                    seedText.text(),
                    defaultLang,
                    seedText.extras(),
                    sectionStorage)));
        }

        return new BlueLangText(declaration.key(), defaultLang, data.toArray(new BlueLangData[0]));
    }

    private BlueLangData toData(BlueLangStoredText storedText) {
        return new BlueLangData(
                storedText.text(),
                storedText.lang(),
                new BlueLangExtraValues(storedText.extras()));
    }

    private Object createValue(Field field, BlueLangText text) {
        if (field.getType() == BlueLangText.class) {
            return text;
        }
        return factories.get(field.getType()).create(text);
    }

    private void assign(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to assign BlueLangText field: "
                    + field.getDeclaringClass().getName() + "#" + field.getName(), e);
        }
    }
}
