package io.fntlv.bluematrix.core.module.registration.instance.parameter.resolver;

import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolver;

public class ModuleRegistryParameterResolver implements ModuleParameterResolver {
    private final ModuleRegistry moduleRegistry;

    public ModuleRegistryParameterResolver(ModuleRegistry moduleRegistry) {
        if (moduleRegistry == null) {
            throw new IllegalArgumentException("moduleRegistry cannot be null");
        }
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType) {
        return ModuleRegistry.class.isAssignableFrom(parameterType)
                && parameterType.isAssignableFrom(moduleRegistry.getClass());
    }

    @Override
    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        return moduleRegistry;
    }
}
