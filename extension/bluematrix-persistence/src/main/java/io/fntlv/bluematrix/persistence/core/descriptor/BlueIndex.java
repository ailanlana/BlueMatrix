package io.fntlv.bluematrix.persistence.core.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BlueIndex {
    String path() default "";

    Class<?> type() default void.class;

    BlueIndexHint.Order order() default BlueIndexHint.Order.ASCENDING;
}
