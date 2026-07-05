package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.event.ModuleEvent;

@FunctionalInterface
public interface ModuleCapabilityBindingHook<C extends ModuleCapabilityContext, S extends ModuleCapabilityState, E extends ModuleEvent> {
    void accept(ModuleCapabilityBinding<C, S> binding, E event);

    static <C extends ModuleCapabilityContext, S extends ModuleCapabilityState, E extends ModuleEvent>
    ModuleCapabilityBindingHook<C, S, E> noop() {
        return (binding, event) -> {
        };
    }
}
