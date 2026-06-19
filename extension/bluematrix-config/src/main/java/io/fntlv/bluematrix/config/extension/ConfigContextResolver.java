package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

public class ConfigContextResolver implements ModuleParameterResolver {
    private final ModuleConfigRegistry configRegistry;

    public ConfigContextResolver(ModuleConfigRegistry configRegistry) {
        if (configRegistry == null) {
            throw new IllegalArgumentException("configRegistry cannot be null");
        }
        this.configRegistry = configRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && ModuleConfigContext.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        return configRegistry.getContext(context.getModuleInfo().id(), context.getModuleClass());
    }
}
