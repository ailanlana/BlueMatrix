package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;

public final class LangExtension implements BlueMatrixExtension {
    private static final String CAPABILITY_ID = "lang";

    private final BlueLangLoader langLoader = new BlueLangLoader();

    public BlueLangLoader langLoader() {
        return langLoader;
    }

    @Override
    public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        ModuleLangInitializer initializer = new ModuleLangInitializer(bootstrap.dataFolder(), langLoader);
        bootstrap.moduleCapability(ModuleCapability.builder(CAPABILITY_ID)
                .onLoadPre((binding, event) -> initializeLang(initializer, event))
                .build());
    }

    private static void initializeLang(ModuleLangInitializer initializer,
                                       ModuleLoadEvent.Pre event) {
        try {
            initializer.initialize(event.getContext());
        } catch (RuntimeException e) {
            event.error(CAPABILITY_ID, "Module language loading failed", e);
        }
    }
}
