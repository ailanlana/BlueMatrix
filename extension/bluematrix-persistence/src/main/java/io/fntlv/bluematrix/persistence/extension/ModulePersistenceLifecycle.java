package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.persistence.core.BlueStorage;
import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSourceContext;

import java.io.File;
import java.util.Set;

final class ModulePersistenceLifecycle {
    private final ModulePersistenceRegistry persistenceRegistry;

    ModulePersistenceLifecycle(ModulePersistenceRegistry persistenceRegistry) {
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        this.persistenceRegistry = persistenceRegistry;
    }

    void register(ModuleCandidate candidate) {
        if (BlueStorageSourceProvider.class.isAssignableFrom(candidate.getModuleClass())) {
            persistenceRegistry.registerStorage(candidate);
        }
    }

    void initialize(ModuleContext context) {
        if (!persistenceRegistry.containsStorage(context)) {
            return;
        }
        BlueStorage storage = persistenceRegistry.getStorage(context);
        BlueStorageSource source = storageSource(context);
        String moduleId = context.getInfo().id();
        File storageRootDirectory = persistenceRegistry.getModuleDataPath(moduleId);
        BlueStorageSourceContext sourceContext = new BlueStorageSourceContext(storageRootDirectory);
        BlueStorageSpec spec = source.toSpec(sourceContext);
        if (spec == null) {
            throw new IllegalArgumentException("storage spec cannot be null");
        }
        storage.initialize(spec.createStorage());
        registerEntityRepositories(context, storage);
    }

    void close(ModuleContext context) {
        if (!persistenceRegistry.containsStorage(context)) {
            return;
        }
        persistenceRegistry.getStorage(context).close();
    }

    private BlueStorageSource storageSource(ModuleContext context) {
        Module module = context.getInstance();
        if (!(module instanceof BlueStorageSourceProvider)) {
            throw new IllegalStateException("Registered persistence module must implement BlueStorageSourceProvider: "
                    + context.getInfo().id() + " (" + module.getClass().getName() + ")");
        }
        BlueStorageSource source = ((BlueStorageSourceProvider) module).getStorageSource();
        if (source == null) {
            throw new IllegalArgumentException("storage source cannot be null");
        }
        return source;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerEntityRepositories(ModuleContext context, BlueStorage storage) {
        Set<Class<?>> entityTypes = context.getReflections().getTypesAnnotatedWith(BlueEntity.class);
        for (Class entityType : entityTypes) {
            storage.repository(entityType);
        }
    }
}
