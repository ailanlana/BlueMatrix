package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public class PersistenceModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(PersistenceModuleListener.class);

    private final ModulePersistenceLifecycle persistenceLifecycle;

    public PersistenceModuleListener(ModulePersistenceRegistry persistenceRegistry) {
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        this.persistenceLifecycle = new ModulePersistenceLifecycle(persistenceRegistry);
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        persistenceLifecycle.register(event.getCandidate());
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        ModuleContext context = event.getContext();
        try {
            persistenceLifecycle.initialize(context);
        } catch (RuntimeException e) {
            event.error("persistence", "Module persistence initialization failed", e);
        }
    }

    @ModuleEventListener
    public void onDisablePost(ModuleDisableEvent.Post event) {
        close(event.getContext());
    }

    @ModuleEventListener
    public void onDisableFailed(ModuleDisableEvent.Failed event) {
        close(event.getContext());
    }

    private void close(ModuleContext context) {
        try {
            persistenceLifecycle.close(context);
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module persistence shutdown failed during disable: [module=%s]",
                    context.getInfo().id()
            ), e);
        }
    }
}
