package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
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
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && BlueStorage.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        if (!BlueStorageSourceProvider.class.isAssignableFrom(context.getModuleClass())) {
            throw new IllegalStateException("BlueStorage injection requires module to implement "
                    + "BlueStorageSourceProvider: " + context.getModuleInfo().id()
                    + " (" + context.getModuleClass().getName() + ")");
        }
        return persistenceRegistry.getStorage(context.getModuleInfo().id());
    }
}
