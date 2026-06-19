package io.fntlv.bluematrix.persistence.core.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BlueEntity {
    String collection();

    Class<? extends BlueEntityCodecFactory> codecFactory() default JacksonJsonCodecFactory.class;

    boolean versioned() default false;
}
