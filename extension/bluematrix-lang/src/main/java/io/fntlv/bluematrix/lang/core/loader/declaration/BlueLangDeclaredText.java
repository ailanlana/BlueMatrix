package io.fntlv.bluematrix.lang.core.loader.declaration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlueLangDeclaredText {
    private final String text;
    private final String lang;
    private final Map<String, Object> extraDefaults;

    public BlueLangDeclaredText(String text, String lang, Map<String, Object> extraDefaults) {
        this.text = text;
        this.lang = lang;
        this.extraDefaults = Collections.unmodifiableMap(new LinkedHashMap<>(extraDefaults));
    }

    public String text() {
        return text;
    }

    public String lang() {
        return lang;
    }

    public Map<String, Object> extraDefaults() {
        return extraDefaults;
    }
}
