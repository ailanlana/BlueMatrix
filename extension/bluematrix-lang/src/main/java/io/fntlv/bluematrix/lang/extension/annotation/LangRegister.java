package io.fntlv.bluematrix.lang.extension.annotation;

import io.fntlv.bluematrix.lang.core.LangType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LangRegister {
    String defaultLang() default LangType.ZH_CN;
}
