package io.fntlv.bluematrix.persistence.core.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BlueCache {
    BlueCachePolicy policy() default BlueCachePolicy.DEFAULT;

    int ttlSeconds() default -1;

    int maxSize() default -1;

    boolean preload() default false;
}
