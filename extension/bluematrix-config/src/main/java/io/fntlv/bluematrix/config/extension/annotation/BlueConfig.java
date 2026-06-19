package io.fntlv.bluematrix.config.extension.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlueConfig {

    String category() default "general";

    String file() default "";

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Field {
        String path();
        String[] defaultValue();
        String comment() default "";
    }
}
