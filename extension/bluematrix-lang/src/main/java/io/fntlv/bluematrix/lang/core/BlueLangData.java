package io.fntlv.bluematrix.lang.core;

import java.util.Collections;

public final class BlueLangData {
    private final String text;
    private final String lang;
    private final BlueLangExtraValues extras;

    public BlueLangData(String text, String lang) {
        this(text, lang, new BlueLangExtraValues(Collections.emptyMap()));
    }

    public BlueLangData(String text, String lang, BlueLangExtraValues extras) {
        this.text = text;
        this.lang = lang;
        this.extras = extras;
    }

    public String text() {
        return text;
    }

    public String lang() {
        return lang;
    }

    public BlueLangExtraValues extras() {
        return extras;
    }
}
