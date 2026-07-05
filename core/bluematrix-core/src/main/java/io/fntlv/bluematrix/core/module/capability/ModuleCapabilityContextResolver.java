package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

import java.util.List;

public final class ModuleCapabilityContextResolver implements ModuleParameterResolver {
    private final ModuleCapabilityRegistry registry;

    public ModuleCapabilityContextResolver(ModuleCapabilityRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context != null && !registry.findByContextType(parameterType).isEmpty();
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        List<ModuleCapability<?, ?>> matches = registry.findByContextType(parameterType);
        if (matches.isEmpty()) {
            throw new IllegalStateException("Unsupported module capability context: " + parameterType.getName());
        }
        for (ModuleCapability<?, ?> capability : matches) {
            if (capability.contains(context.id())) {
                return capability.context(context.id());
            }
        }
        throw new IllegalStateException("Module capability context is not registered for module: "
                + parameterType.getName() + " / " + context.id());
    }
}
