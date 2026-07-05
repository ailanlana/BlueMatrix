package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;
import io.fntlv.bluematrix.lang.extension.annotation.LangRegister;

import java.io.File;
import java.util.Set;

final class ModuleLangInitializer {
    private final File dataFolder;
    private final BlueLangLoader langLoader;

    ModuleLangInitializer(File dataFolder, BlueLangLoader langLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (langLoader == null) {
            throw new IllegalArgumentException("langLoader cannot be null");
        }
        this.dataFolder = dataFolder;
        this.langLoader = langLoader;
    }

    void initialize(ModuleContext context) {
        Set<Class<?>> langClasses = context.getReflections().getTypesAnnotatedWith(LangRegister.class);
        for (Class<?> langClass : langClasses) {
            initialize(context, langClass);
        }
    }

    private void initialize(ModuleContext context, Class<?> langClass) {
        LangRegister register = langClass.getAnnotation(LangRegister.class);
        if (register == null) {
            return;
        }
        langLoader.load(moduleFolder(context), register.defaultLang(), langClass);
    }

    private File moduleFolder(ModuleContext context) {
        return new File(dataFolder, context.id());
    }
}
