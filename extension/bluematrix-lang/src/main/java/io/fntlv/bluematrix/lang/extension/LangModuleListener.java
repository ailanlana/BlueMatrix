package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;
import io.fntlv.bluematrix.lang.extension.annotation.LangRegister;

import java.io.File;
import java.util.Set;

public class LangModuleListener {
    private final File dataFolder;
    private final BlueLangLoader langLoader;

    public LangModuleListener(File dataFolder, BlueLangLoader langLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (langLoader == null) {
            throw new IllegalArgumentException("langLoader cannot be null");
        }
        this.dataFolder = dataFolder;
        this.langLoader = langLoader;
    }

    @ModuleEventListener
    public void onLoadPre(ModuleLoadEvent.Pre event) {
        try {
            load(event.getContext());
        } catch (RuntimeException e) {
            event.error("lang", "Module language loading failed", e);
        }
    }

    private void load(ModuleContext context) {
        Set<Class<?>> langClasses = context.getReflections().getTypesAnnotatedWith(LangRegister.class);
        for (Class<?> langClass : langClasses) {
            loadLangClass(context, langClass);
        }
    }

    private void loadLangClass(ModuleContext context, Class<?> langClass) {
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
