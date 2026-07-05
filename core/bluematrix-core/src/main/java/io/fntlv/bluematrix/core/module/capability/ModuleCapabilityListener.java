package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.event.ModuleEvent;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public final class ModuleCapabilityListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ModuleCapabilityListener.class);

    private final ModuleCapabilityRegistry registry;

    public ModuleCapabilityListener(ModuleCapabilityRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        for (ModuleCapability<?, ?> capability : registry.capabilities()) {
            capability.onRegisterPre(event);
        }
    }

    @ModuleEventListener
    public void onRegisterPost(ModuleRegisterEvent.Post event) {
        dispatchSafely(event, capability -> capability.onRegisterPost(event));
    }

    @ModuleEventListener
    public void onLoadPre(ModuleLoadEvent.Pre event) {
        for (ModuleCapability<?, ?> capability : registry.capabilities()) {
            try {
                capability.onLoadPre(event);
            } catch (RuntimeException e) {
                event.error(capability.id(), "Module capability load failed", e);
            }
        }
    }

    @ModuleEventListener
    public void onLoadPost(ModuleLoadEvent.Post event) {
        dispatchSafely(event, capability -> capability.onLoadPost(event));
    }

    @ModuleEventListener
    public void onLoadFailed(ModuleLoadEvent.Failed event) {
        dispatchSafely(event, capability -> capability.onLoadFailed(event));
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        for (ModuleCapability<?, ?> capability : registry.capabilities()) {
            try {
                capability.onEnablePre(event);
            } catch (RuntimeException e) {
                event.error(capability.id(), "Module capability enable failed", e);
            }
        }
    }

    @ModuleEventListener
    public void onEnablePost(ModuleEnableEvent.Post event) {
        dispatchSafely(event, capability -> capability.onEnablePost(event));
    }

    @ModuleEventListener
    public void onEnableSkipped(ModuleEnableEvent.Skipped event) {
        dispatchSafely(event, capability -> capability.onEnableSkipped(event));
    }

    @ModuleEventListener
    public void onEnableFailed(ModuleEnableEvent.Failed event) {
        dispatchSafely(event, capability -> capability.onEnableFailed(event));
    }

    @ModuleEventListener
    public void onDisablePre(ModuleDisableEvent.Pre event) {
        dispatchSafely(event, capability -> capability.onDisablePre(event));
    }

    @ModuleEventListener
    public void onDisablePost(ModuleDisableEvent.Post event) {
        dispatchSafely(event, capability -> capability.onDisablePost(event));
    }

    @ModuleEventListener
    public void onDisableFailed(ModuleDisableEvent.Failed event) {
        dispatchSafely(event, capability -> capability.onDisableFailed(event));
    }

    private void dispatchSafely(ModuleEvent event, CapabilityDispatcher dispatcher) {
        for (ModuleCapability<?, ?> capability : registry.capabilities()) {
            try {
                dispatcher.dispatch(capability);
            } catch (RuntimeException e) {
                LOGGER.error(String.format(
                        "Module capability event failed: [capability=%s], [event=%s], [module=%s]",
                        capability.id(),
                        event.getClass().getSimpleName(),
                        moduleId(event)
                ), e);
            }
        }
    }

    private String moduleId(ModuleEvent event) {
        if (event instanceof ModuleRegisterEvent.Post) {
            return ((ModuleRegisterEvent.Post) event).getContext().id();
        }
        if (event instanceof ModuleLoadEvent) {
            return ((ModuleLoadEvent) event).getContext().id();
        }
        if (event instanceof ModuleEnableEvent) {
            return ((ModuleEnableEvent) event).getContext().id();
        }
        if (event instanceof ModuleDisableEvent) {
            return ((ModuleDisableEvent) event).getContext().id();
        }
        return "unknown";
    }

    @FunctionalInterface
    private interface CapabilityDispatcher {
        void dispatch(ModuleCapability<?, ?> capability);
    }
}
