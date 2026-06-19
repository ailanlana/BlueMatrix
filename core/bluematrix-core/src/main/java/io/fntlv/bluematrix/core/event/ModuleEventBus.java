package io.fntlv.bluematrix.core.event;

public interface ModuleEventBus {
    void registerListener(Object listener);

    void publish(ModuleEvent event);
}
