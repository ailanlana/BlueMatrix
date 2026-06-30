package io.fntlv.bluematrix.lang.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueLangDataTest {

    @Test
    void createsFromTextAndLang() {
        BlueLangData data = new BlueLangData("Hello", LangType.EN_US);

        assertEquals("Hello", data.text());
        assertEquals(LangType.EN_US, data.lang());
        assertEquals("", data.extras().getString("missing", ""));
    }

    @Test
    void createsFromTextLangAndExtras() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runCommand", "/menu");
        BlueLangData data = new BlueLangData("Hello", LangType.EN_US, new BlueLangExtraValues(values));

        assertEquals("/menu", data.extras().getString("runCommand", ""));
    }

}
