package io.fntlv.bluematrix.persistence.extension;

import br.com.finalcraft.everydatabase.manager.sync.CacheSyncTransport;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.persistence.core.cache.BlueCacheSyncCoordinator;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinitionFactory;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSource;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSourceContext;

import java.io.File;
import java.util.Set;

final class ModulePersistenceInitializer {
    private static final String MODULES_DIRECTORY_NAME = "modules";

    private final File dataFolder;
    private final BlueDataDefinitionFactory definitionFactory;

    ModulePersistenceInitializer(File dataFolder) {
        this(dataFolder, new BlueDataDefinitionFactory());
    }

    ModulePersistenceInitializer(File dataFolder, BlueDataDefinitionFactory definitionFactory) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (definitionFactory == null) {
            throw new IllegalArgumentException("definitionFactory cannot be null");
        }
        this.dataFolder = dataFolder;
        this.definitionFactory = definitionFactory;
    }

    void initialize(ModuleContext context, BlueStorage storage) {
        initialize(context, storage, null);
    }

    void initialize(ModuleContext context, BlueStorage storage, BlueCacheSyncCoordinator cacheSyncCoordinator) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        BlueStorageSource source = ((BlueStorageSourceProvider) context.getInstance()).getStorageSource();
        if (source == null) {
            throw new IllegalArgumentException("storage source cannot be null");
        }
        File storageRootDirectory = storageRootDirectory(context.id());
        BlueStorageSourceContext sourceContext = new BlueStorageSourceContext(storageRootDirectory);
        BlueStorageSpec spec = source.toSpec(sourceContext);
        if (spec == null) {
            throw new IllegalArgumentException("storage spec cannot be null");
        }
        storage.initialize(spec.createStorage());
        registerDataDefinitions(context, storage);
        startCacheSync(context, storage, cacheSyncCoordinator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerDataDefinitions(ModuleContext context, BlueStorage storage) {
        Set<Class<?>> entityTypes = context.getReflections().getTypesAnnotatedWith(BlueEntity.class);
        for (Class entityType : entityTypes) {
            BlueDataDefinition definition = definitionFactory.create(entityType, storage);
            storage.registry().register(definition);
        }
    }

    private File storageRootDirectory(String moduleId) {
        return new File(new File(new File(dataFolder, MODULES_DIRECTORY_NAME), moduleId), "data");
    }

    private void startCacheSync(ModuleContext context,
                                BlueStorage storage,
                                BlueCacheSyncCoordinator cacheSyncCoordinator) {
        if (!(context.getInstance() instanceof BlueCacheSyncTransportProvider) || cacheSyncCoordinator == null) {
            return;
        }
        CacheSyncTransport transport =
                ((BlueCacheSyncTransportProvider) context.getInstance()).getCacheSyncTransport();
        cacheSyncCoordinator.start(storage, transport);
    }
}
