package io.fntlv.bluematrix.core.module.instance.parameter.resolver;

import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

public class ModuleEventBusParameterResolver implements ModuleParameterResolver {
    private final ModuleEventBus eventBus;

    public ModuleEventBusParameterResolver(ModuleEventBus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus cannot be null");
        }
        this.eventBus = eventBus;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && ModuleEventBus.class.isAssignableFrom(parameterType)
                && parameterType.isAssignableFrom(eventBus.getClass());
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        return eventBus;
    }
}
