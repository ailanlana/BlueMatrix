package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.persistence.core.BlueStorage;

public class PersistenceStorageResolver implements ModuleParameterResolver {
    private final ModulePersistenceRegistry persistenceRegistry;

    public PersistenceStorageResolver(ModulePersistenceRegistry persistenceRegistry) {
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        this.persistenceRegistry = persistenceRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType) {
        return BlueStorage.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        if (!BlueStorageSourceProvider.class.isAssignableFrom(candidate.getModuleClass())) {
            throw new IllegalStateException("BlueStorage injection requires module to implement "
                    + "BlueStorageSourceProvider: " + candidate.getModuleInfo().id()
                    + " (" + candidate.getModuleClass().getName() + ")");
        }
        return persistenceRegistry.getStorage(candidate);
    }
}
