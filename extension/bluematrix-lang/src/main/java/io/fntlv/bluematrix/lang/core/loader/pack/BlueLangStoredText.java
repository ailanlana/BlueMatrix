package io.fntlv.bluematrix.lang.core.loader.pack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlueLangStoredText {
    private final String text;
    private final String lang;
    private final Map<String, Object> extras;

    public BlueLangStoredText(String text, String lang, Map<String, Object> extras) {
        this.text = text;
        this.lang = lang;
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(extras));
    }

    public String text() {
        return text;
    }

    public String lang() {
        return lang;
    }

    public Map<String, Object> extras() {
        return extras;
    }
}
