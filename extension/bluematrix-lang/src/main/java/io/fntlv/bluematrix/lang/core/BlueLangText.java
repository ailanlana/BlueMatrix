package io.fntlv.bluematrix.lang.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BlueLangText {
    public static final String DEFAULT_LANG = LangType.EN_US;

    private final String key;
    private final String defaultLang;
    private final Map<String, BlueLangData> dataByLang = new LinkedHashMap<>();
    private final BlueLangData defaultData;

    public BlueLangText(BlueLangData... data) {
        this("", data);
    }

    public BlueLangText(String key, BlueLangData... data) {
        this(key, DEFAULT_LANG, data);
    }

    public BlueLangText(String key, String defaultLang, BlueLangData... data) {
        this.key = key;
        this.defaultLang = defaultLang;
        for (BlueLangData item : data) {
            add(item);
        }
        BlueLangData resolvedDefault = data(defaultLang);
        this.defaultData = resolvedDefault == null ? data[0] : resolvedDefault;
    }

    public String key() {
        return key;
    }

    public String defaultLang() {
        return defaultLang;
    }

    public String text() {
        return defaultData.text();
    }

    public String text(String lang) {
        BlueLangData data = data(lang);
        return data == null ? defaultData.text() : data.text();
    }

    public boolean hasLang(String lang) {
        return dataByLang.containsKey(lang);
    }

    public BlueLangData data() {
        return defaultData;
    }

    public BlueLangData data(String lang) {
        return dataByLang.get(lang);
    }

    public Set<String> langs() {
        return dataByLang.keySet();
    }

    private void add(BlueLangData data) {
        if (dataByLang.containsKey(data.lang())) {
            throw new IllegalArgumentException("Duplicate lang: " + data.lang());
        }
        dataByLang.put(data.lang(), data);
    }
}
