package io.fntlv.bluematrix.core.module.registration.instance.parameter.resolver;

import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolver;

public class ModuleEventBusParameterResolver implements ModuleParameterResolver {
    private final ModuleEventBus eventBus;

    public ModuleEventBusParameterResolver(ModuleEventBus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus cannot be null");
        }
        this.eventBus = eventBus;
    }

    @Override
    public boolean supports(Class<?> parameterType) {
        return ModuleEventBus.class.isAssignableFrom(parameterType)
                && parameterType.isAssignableFrom(eventBus.getClass());
    }

    @Override
    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        return eventBus;
    }
}
