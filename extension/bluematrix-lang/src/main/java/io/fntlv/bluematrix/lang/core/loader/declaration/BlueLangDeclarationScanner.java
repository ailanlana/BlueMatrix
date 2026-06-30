package io.fntlv.bluematrix.lang.core.loader.declaration;

import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.annotation.BlueLang;
import io.fntlv.bluematrix.lang.core.annotation.BlueLangKey;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlueLangDeclarationScanner {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(BlueLangDeclarationScanner.class);

    private final Set<Class<?>> registeredTextTypes;

    public BlueLangDeclarationScanner(Set<Class<?>> registeredTextTypes) {
        this.registeredTextTypes = registeredTextTypes;
    }

    public List<BlueLangDeclaration> scan(Class<?> langClass) {
        List<BlueLangDeclaration> declarations = new ArrayList<>();
        for (Field field : langClass.getDeclaredFields()) {
            BlueLang[] annotations = field.getAnnotationsByType(BlueLang.class);
            if (annotations.length == 0) {
                continue;
            }
            if (field.getType() != BlueLangText.class && !registeredTextTypes.contains(field.getType())) {
                LOGGER.warn("@BlueLang must be declared on BlueLangText field or registered lang text field: {}#{}",
                        langClass.getName(),
                        field.getName());
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                LOGGER.warn("@BlueLang BlueLangText field must be static when loading by class: {}#{}",
                        langClass.getName(),
                        field.getName());
                continue;
            }
            declarations.add(new BlueLangDeclaration(
                    field,
                    resolveKey(field),
                    field.getType() != BlueLangText.class,
                    declaredTexts(annotations)));
        }
        return declarations;
    }

    private List<BlueLangDeclaredText> declaredTexts(BlueLang[] annotations) {
        List<BlueLangDeclaredText> texts = new ArrayList<>();
        for (BlueLang annotation : annotations) {
            texts.add(new BlueLangDeclaredText(
                    annotation.text(),
                    annotation.lang(),
                    extraDefaults(annotation.extras())));
        }
        return texts;
    }

    private Map<String, Object> extraDefaults(BlueLang.Extra[] extras) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (BlueLang.Extra extra : extras) {
            if (extra.value().length == 0) {
                values.put(extra.key(), "");
                continue;
            }
            if (extra.value().length == 1) {
                values.put(extra.key(), extra.value()[0]);
                continue;
            }
            List<String> list = new ArrayList<>();
            for (String value : extra.value()) {
                list.add(value);
            }
            values.put(extra.key(), list);
        }
        return values;
    }

    private String resolveKey(Field field) {
        String classKey = resolveClassKey(field.getDeclaringClass());
        String fieldKey = resolveFieldKey(field);
        if (classKey.isEmpty()) {
            return fieldKey;
        }
        return classKey + "." + fieldKey;
    }

    private String resolveClassKey(Class<?> langClass) {
        BlueLangKey key = langClass.getAnnotation(BlueLangKey.class);
        return key == null ? "" : key.value();
    }

    private String resolveFieldKey(Field field) {
        BlueLangKey key = field.getAnnotation(BlueLangKey.class);
        if (key != null) {
            return key.value();
        }
        return field.getName().replace("__", ".").toLowerCase();
    }
}
