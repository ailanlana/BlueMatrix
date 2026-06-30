package io.fntlv.bluematrix.lang.core.annotation;

import io.fntlv.bluematrix.lang.core.LangType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(BlueMultiLang.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BlueLang {
    String text() default "";

    String lang() default LangType.EN_US;

    Extra[] extras() default {};

    @interface Extra {
        String key();

        String[] value() default {};
    }
}
