package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.event.ModuleEvent;

@FunctionalInterface
public interface ModuleCapabilityEventHook<E extends ModuleEvent> {
    void accept(E event);

    static <E extends ModuleEvent> ModuleCapabilityEventHook<E> noop() {
        return event -> {
        };
    }
}
