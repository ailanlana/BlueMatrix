package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolver;

public class ConfigContextResolver implements ModuleParameterResolver {
    private final ModuleConfigRegistry configRegistry;

    public ConfigContextResolver(ModuleConfigRegistry configRegistry) {
        if (configRegistry == null) {
            throw new IllegalArgumentException("configRegistry cannot be null");
        }
        this.configRegistry = configRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType) {
        return ModuleConfigContext.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        return configRegistry.getContext(candidate);
    }
}
