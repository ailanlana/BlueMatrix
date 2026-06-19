package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainerEvent;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.persistence.core.BlueStorage;
import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSourceContext;

import java.io.File;
import java.util.Set;

public class PersistenceModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(PersistenceModuleListener.class);

    private final ModulePersistenceRegistry persistenceRegistry;

    public PersistenceModuleListener(ModulePersistenceRegistry persistenceRegistry) {
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        this.persistenceRegistry = persistenceRegistry;
    }

    @ModuleEventListener
    public void onContainerCreated(BlueMatrixContainerEvent.Created event) {
        event.getParameterResolvers().registerIfAbsent(new PersistenceStorageResolver(persistenceRegistry));
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        if (BlueStorageSourceProvider.class.isAssignableFrom(event.getCandidate().getModuleClass())) {
            persistenceRegistry.registerStorage(event.getCandidate());
        }
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        ModuleContext context = event.getContext();
        Module module = context.getInstance();
        if (!persistenceRegistry.containsStorage(context)) {
            return;
        }
        try {
            BlueStorage storage = persistenceRegistry.getStorage(context);
            BlueStorageSource source = ((BlueStorageSourceProvider) module).getStorageSource();
            if (source == null) {
                throw new IllegalArgumentException("storage source cannot be null");
            }
            String moduleId = context.getInfo().id();
            File storageRootDirectory = persistenceRegistry.getModuleDataPath(moduleId);
            BlueStorageSourceContext sourceContext = new BlueStorageSourceContext(storageRootDirectory);
            BlueStorageSpec spec = source.toSpec(sourceContext);
            if (spec == null) {
                throw new IllegalArgumentException("storage spec cannot be null");
            }
            storage.initialize(spec.createStorage());
            registerEntityRepositories(context, storage);
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
        if (!persistenceRegistry.containsStorage(context)) {
            return;
        }
        try {
            persistenceRegistry.getStorage(context).close();
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module persistence shutdown failed during disable: [module=%s]",
                    context.getInfo().id()
            ), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerEntityRepositories(ModuleContext context, BlueStorage storage) {
        Set<Class<?>> entityTypes = context.getReflections().getTypesAnnotatedWith(BlueEntity.class);
        for (Class entityType : entityTypes) {
            storage.repository(entityType);
        }
    }
}
