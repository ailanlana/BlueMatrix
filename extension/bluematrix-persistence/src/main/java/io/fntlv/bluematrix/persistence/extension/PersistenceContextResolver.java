package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

public class PersistenceContextResolver implements ModuleParameterResolver {
    private final ModulePersistenceRegistry persistenceRegistry;

    public PersistenceContextResolver(ModulePersistenceRegistry persistenceRegistry) {
        if (persistenceRegistry == null) {
            throw new IllegalArgumentException("persistenceRegistry cannot be null");
        }
        this.persistenceRegistry = persistenceRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && ModulePersistenceContext.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        if (!BlueStorageSourceProvider.class.isAssignableFrom(context.getModuleClass())) {
            throw new IllegalStateException("ModulePersistenceContext injection requires module to implement "
                    + "BlueStorageSourceProvider: " + context.id()
                    + " (" + context.getModuleClass().getName() + ")");
        }
        return persistenceRegistry.get(context.id());
    }
}
