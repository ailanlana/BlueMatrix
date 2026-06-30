package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.lang.core.BlueLangData;
import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.LangType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueLangTextTest {

    @Test
    void readsTextByLang() {
        BlueLangText text = new BlueLangText(
                new BlueLangData("你好", LangType.ZH_CN),
                new BlueLangData("Hello", LangType.EN_US)
        );

        assertEquals("", text.key());
        assertEquals("你好", text.text(LangType.ZH_CN));
        assertEquals("Hello", text.text(LangType.EN_US));
    }

    @Test
    void readsKey() {
        BlueLangText text = new BlueLangText(
                "command.help",
                new BlueLangData("你好", LangType.ZH_CN),
                new BlueLangData("Hello", LangType.EN_US)
        );

        assertEquals("command.help", text.key());
    }

    @Test
    void readsDefaultLangText() {
        BlueLangText text = new BlueLangText(
                new BlueLangData("你好", LangType.ZH_CN),
                new BlueLangData("Hello", BlueLangText.DEFAULT_LANG)
        );

        assertEquals("Hello", text.text());
    }

    @Test
    void fallsBackToDefaultTextWhenLangMissing() {
        BlueLangText text = new BlueLangText(
                new BlueLangData("你好", LangType.ZH_CN),
                new BlueLangData("Hello", LangType.EN_US)
        );

        assertEquals("Hello", text.text("ja_JP"));
    }

    @Test
    void usesFirstDataAsDefaultWhenDefaultLangIsMissing() {
        BlueLangText text = new BlueLangText(
                new BlueLangData("你好", LangType.ZH_CN)
        );

        assertEquals("你好", text.text("ja_JP"));
    }

    @Test
    void checksLangPresence() {
        BlueLangText text = new BlueLangText(
                new BlueLangData("Hello", LangType.EN_US)
        );

        assertTrue(text.hasLang(LangType.EN_US));
        assertFalse(text.hasLang(LangType.ZH_CN));
    }

    @Test
    void readsDataByLang() {
        BlueLangData data = new BlueLangData("Hello", LangType.EN_US);
        BlueLangText text = new BlueLangText(data);

        assertSame(data, text.data(LangType.EN_US));
        assertNull(text.data(LangType.ZH_CN));
    }

    @Test
    void readsDefaultDataAndLangs() {
        BlueLangData data = new BlueLangData("Hello", LangType.EN_US);
        BlueLangText text = new BlueLangText("command.help", LangType.EN_US, data);

        assertEquals(LangType.EN_US, text.defaultLang());
        assertSame(data, text.data());
        assertTrue(text.langs().contains(LangType.EN_US));
    }

    @Test
    void rejectsInvalidConstructorInput() {
        assertThrows(IllegalArgumentException.class, () -> new BlueLangText(
                new BlueLangData("Hello", LangType.EN_US),
                new BlueLangData("Hi", LangType.EN_US)
        ));
    }
}
