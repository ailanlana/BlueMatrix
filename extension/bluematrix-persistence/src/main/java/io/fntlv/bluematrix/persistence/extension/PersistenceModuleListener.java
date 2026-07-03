package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinitionFactory;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSource;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSourceContext;

import java.io.File;
import java.util.Set;

public class PersistenceModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(PersistenceModuleListener.class);
    private static final String MODULES_DIRECTORY_NAME = "modules";

    private final File dataFolder;
    private final ModulePersistenceRegistry persistenceRegistry;
    private final BlueDataDefinitionFactory definitionFactory;

    public PersistenceModuleListener(File dataFolder, ModulePersistenceRegistry persistenceRegistry) {
        this(dataFolder, persistenceRegistry, new BlueDataDefinitionFactory());
    }

    PersistenceModuleListener(File dataFolder,
                              ModulePersistenceRegistry persistenceRegistry,
                              BlueDataDefinitionFactory definitionFactory) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        if (definitionFactory == null) {
            throw new IllegalArgumentException("definitionFactory cannot be null");
        }
        this.dataFolder = dataFolder;
        this.persistenceRegistry = persistenceRegistry;
        this.definitionFactory = definitionFactory;
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        if (BlueStorageSourceProvider.class.isAssignableFrom(event.getCandidate().getModuleClass())) {
            persistenceRegistry.register(event.getCandidate().id(),
                    new DefaultModulePersistenceContext(event.getCandidate().id()));
        }
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        ModuleContext context = event.getContext();
        try {
            initialize(context);
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
            closePersistence(context);
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module persistence shutdown failed during disable: [module=%s]",
                    context.id()
            ), e);
        }
    }

    private void initialize(ModuleContext context) {
        if (!persistenceRegistry.contains(context.id())) {
            return;
        }
        DefaultModulePersistenceContext persistenceContext = persistenceRegistry.get(context.id());
        BlueStorage storage = persistenceContext.storage();
        BlueStorageSource source = storageSource(context);
        String moduleId = context.id();
        File storageRootDirectory = moduleDataPath(moduleId);
        BlueStorageSourceContext sourceContext = new BlueStorageSourceContext(storageRootDirectory);
        BlueStorageSpec spec = source.toSpec(sourceContext);
        if (spec == null) {
            throw new IllegalArgumentException("storage spec cannot be null");
        }
        storage.initialize(spec.createStorage());
        registerDataDefinitions(context, storage);
    }

    private void closePersistence(ModuleContext context) {
        if (!persistenceRegistry.contains(context.id())) {
            return;
        }
        try {
            persistenceRegistry.get(context.id()).storage().close();
        } finally {
            persistenceRegistry.remove(context.id());
        }
    }

    private BlueStorageSource storageSource(ModuleContext context) {
        Module module = context.getInstance();
        if (!(module instanceof BlueStorageSourceProvider)) {
            throw new IllegalStateException("Registered persistence module must implement BlueStorageSourceProvider: "
                    + context.id() + " (" + module.getClass().getName() + ")");
        }
        BlueStorageSource source = ((BlueStorageSourceProvider) module).getStorageSource();
        if (source == null) {
            throw new IllegalArgumentException("storage source cannot be null");
        }
        return source;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerDataDefinitions(ModuleContext context, BlueStorage storage) {
        Set<Class<?>> entityTypes = context.getReflections().getTypesAnnotatedWith(BlueEntity.class);
        for (Class entityType : entityTypes) {
            BlueDataDefinition definition = definitionFactory.create(entityType, storage);
            storage.registry().register(definition);
        }
    }

    private File moduleDataPath(String moduleId) {
        return new File(new File(new File(dataFolder, MODULES_DIRECTORY_NAME), moduleId), "data");
    }
}
