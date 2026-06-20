package io.fntlv.bluematrix.core.module.instance.parameter.resolver;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

public class ModuleInstanceFactoryParameterResolver implements ModuleParameterResolver {
    private final ModuleInstanceFactory instanceFactory;

    public ModuleInstanceFactoryParameterResolver(ModuleInstanceFactory instanceFactory) {
        if (instanceFactory == null) {
            throw new IllegalArgumentException("instanceFactory cannot be null");
        }
        this.instanceFactory = instanceFactory;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && ModuleInstanceFactory.class.isAssignableFrom(parameterType)
                && parameterType.isAssignableFrom(instanceFactory.getClass());
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        return instanceFactory;
    }
}
