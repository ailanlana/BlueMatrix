package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.lang.core.annotation.BlueLangKey;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueLangKeyTest {

    @Test
    void isRuntimeTypeAndFieldAnnotation() {
        Retention retention = BlueLangKey.class.getAnnotation(Retention.class);
        Target target = BlueLangKey.class.getAnnotation(Target.class);

        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[]{ElementType.TYPE, ElementType.FIELD}, target.value());
    }

    @Test
    void readsClassAndFieldKeys() throws Exception {
        Field field = Example.class.getDeclaredField("help");

        assertEquals("command", Example.class.getAnnotation(BlueLangKey.class).value());
        assertEquals("help", field.getAnnotation(BlueLangKey.class).value());
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class Example {
        @BlueLangKey("help")
        private Object help;
    }
}
