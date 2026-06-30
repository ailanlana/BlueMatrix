package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.lang.core.annotation.BlueLang;
import io.fntlv.bluematrix.lang.core.LangType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueLangTest {

    @Test
    void isRuntimeFieldAnnotation() {
        Retention retention = BlueLang.class.getAnnotation(Retention.class);
        Target target = BlueLang.class.getAnnotation(Target.class);

        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[]{ElementType.FIELD}, target.value());
    }

    @Test
    void readsExplicitTextAndLangFromField() throws Exception {
        Field field = Example.class.getDeclaredField("explicit");
        BlueLang blueLang = field.getAnnotation(BlueLang.class);

        assertEquals("Hello", blueLang.text());
        assertEquals(LangType.EN_US, blueLang.lang());
    }

    @Test
    void readsDefaultTextAndLangFromField() throws Exception {
        Field field = Example.class.getDeclaredField("defaults");
        BlueLang blueLang = field.getAnnotation(BlueLang.class);

        assertEquals("", blueLang.text());
        assertEquals(LangType.EN_US, blueLang.lang());
    }

    @Test
    void readsRepeatableTextAndLangFromField() throws Exception {
        Field field = Example.class.getDeclaredField("repeatable");
        BlueLang[] blueLangs = field.getAnnotationsByType(BlueLang.class);

        assertEquals(2, blueLangs.length);
        assertEquals("你好", blueLangs[0].text());
        assertEquals(LangType.ZH_CN, blueLangs[0].lang());
        assertEquals("Hello", blueLangs[1].text());
        assertEquals(LangType.EN_US, blueLangs[1].lang());
    }

    @Test
    void readsExtrasFromField() throws Exception {
        Field field = Example.class.getDeclaredField("extra");
        BlueLang blueLang = field.getAnnotation(BlueLang.class);

        assertEquals(2, blueLang.extras().length);
        assertEquals("runCommand", blueLang.extras()[0].key());
        assertArrayEquals(new String[]{"/menu"}, blueLang.extras()[0].value());
        assertEquals("hoverText", blueLang.extras()[1].key());
        assertArrayEquals(new String[]{"Click here", "Open menu"}, blueLang.extras()[1].value());
    }

    @SuppressWarnings("unused")
    private static final class Example {
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private Object explicit;

        @BlueLang
        private Object defaults;

        @BlueLang(text = "你好", lang = LangType.ZH_CN)
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private Object repeatable;

        @BlueLang(
                text = "Open",
                extras = {
                        @BlueLang.Extra(key = "runCommand", value = "/menu"),
                        @BlueLang.Extra(key = "hoverText", value = {"Click here", "Open menu"})
                }
        )
        private Object extra;
    }
}
