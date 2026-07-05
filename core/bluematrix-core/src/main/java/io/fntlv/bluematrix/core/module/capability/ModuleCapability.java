package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ModuleCapability<C extends ModuleCapabilityContext, S extends ModuleCapabilityState> {
    private final String id;
    private final Class<C> contextType;
    private final boolean contextExposed;
    private final Predicate<ModuleCandidate> enabledWhen;
    private final Function<String, S> stateFactory;
    private final BiFunction<String, S, C> contextFactory;
    private final Map<String, ModuleCapabilityBinding<C, S>> bindings =
            Collections.synchronizedMap(new HashMap<>());

    private final ModuleCapabilityEventHook<ModuleRegisterEvent.Pre> registerPreHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleRegisterEvent.Post> registerPostHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Pre> loadPreHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Post> loadPostHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Failed> loadFailedHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Pre> enablePreHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Post> enablePostHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Skipped> enableSkippedHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Failed> enableFailedHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Pre> disablePreHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Post> disablePostHook;
    private final ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Failed> disableFailedHook;

    ModuleCapability(ModuleCapabilityBuilder<C, S> builder) {
        this.id = builder.id();
        this.contextType = builder.contextType();
        this.contextExposed = builder.contextExposed();
        this.enabledWhen = builder.enabledWhen();
        this.stateFactory = builder.stateFactory();
        this.contextFactory = builder.contextFactory();
        this.registerPreHook = builder.registerPreHook();
        this.registerPostHook = builder.registerPostHook();
        this.loadPreHook = builder.loadPreHook();
        this.loadPostHook = builder.loadPostHook();
        this.loadFailedHook = builder.loadFailedHook();
        this.enablePreHook = builder.enablePreHook();
        this.enablePostHook = builder.enablePostHook();
        this.enableSkippedHook = builder.enableSkippedHook();
        this.enableFailedHook = builder.enableFailedHook();
        this.disablePreHook = builder.disablePreHook();
        this.disablePostHook = builder.disablePostHook();
        this.disableFailedHook = builder.disableFailedHook();
    }

    public static <C extends ModuleCapabilityContext, S extends ModuleCapabilityState>
    ModuleCapabilityBuilder<C, S> builder(String id) {
        return new ModuleCapabilityBuilder<>(id);
    }

    public String id() {
        return id;
    }

    public Class<C> contextType() {
        return contextType;
    }

    public boolean supports(Class<?> requestedContextType) {
        return contextExposed && requestedContextType != null && contextType().isAssignableFrom(requestedContextType);
    }

    public boolean contains(String moduleId) {
        return moduleId != null && bindings.containsKey(moduleId);
    }

    public Optional<ModuleCapabilityBinding<C, S>> findBinding(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(bindings.get(moduleId));
    }

    public ModuleCapabilityBinding<C, S> binding(String moduleId) {
        return findBinding(moduleId).orElseThrow(() -> new IllegalStateException(
                "Module capability binding is not registered: " + id() + " / " + moduleId
        ));
    }

    public Optional<C> findContext(String moduleId) {
        return findBinding(moduleId).map(ModuleCapabilityBinding::context);
    }

    public C context(String moduleId) {
        return binding(moduleId).context();
    }

    public S getState(String moduleId) {
        return binding(moduleId).state();
    }

    public void remove(String moduleId) {
        bindings.remove(moduleId);
    }

    void onRegisterPre(ModuleRegisterEvent.Pre event) {
        registerPreHook.accept(event);
        ModuleCandidate candidate = event.getCandidate();
        if (!enabledWhen.test(candidate)) {
            return;
        }
        String moduleId = candidate.id();
        synchronized (bindings) {
            if (bindings.containsKey(moduleId)) {
                return;
            }
            S state = stateFactory.apply(moduleId);
            if (state == null) {
                throw new IllegalStateException("Module capability state cannot be null: " + id());
            }
            C context = contextFactory.apply(moduleId, state);
            if (context == null) {
                throw new IllegalStateException("Module capability context cannot be null: " + id());
            }
            bindings.put(moduleId, new ModuleCapabilityBinding<>(moduleId, context, state));
        }
    }

    void onRegisterPost(ModuleRegisterEvent.Post event) {
        dispatch(event.getContext().id(), event, registerPostHook);
    }

    void onLoadPre(ModuleLoadEvent.Pre event) {
        dispatch(event.getContext().id(), event, loadPreHook);
    }

    void onLoadPost(ModuleLoadEvent.Post event) {
        dispatch(event.getContext().id(), event, loadPostHook);
    }

    void onLoadFailed(ModuleLoadEvent.Failed event) {
        dispatch(event.getContext().id(), event, loadFailedHook);
    }

    void onEnablePre(ModuleEnableEvent.Pre event) {
        dispatch(event.getContext().id(), event, enablePreHook);
    }

    void onEnablePost(ModuleEnableEvent.Post event) {
        dispatch(event.getContext().id(), event, enablePostHook);
    }

    void onEnableSkipped(ModuleEnableEvent.Skipped event) {
        dispatch(event.getContext().id(), event, enableSkippedHook);
    }

    void onEnableFailed(ModuleEnableEvent.Failed event) {
        dispatch(event.getContext().id(), event, enableFailedHook);
    }

    void onDisablePre(ModuleDisableEvent.Pre event) {
        dispatch(event.getContext().id(), event, disablePreHook);
    }

    void onDisablePost(ModuleDisableEvent.Post event) {
        try {
            dispatch(event.getContext().id(), event, disablePostHook);
        } finally {
            remove(event.getContext().id());
        }
    }

    void onDisableFailed(ModuleDisableEvent.Failed event) {
        try {
            dispatch(event.getContext().id(), event, disableFailedHook);
        } finally {
            remove(event.getContext().id());
        }
    }

    private <E extends io.fntlv.bluematrix.core.event.ModuleEvent> void dispatch(
            String moduleId,
            E event,
            ModuleCapabilityBindingHook<C, S, E> hook) {
        ModuleCapabilityBinding<C, S> binding = bindings.get(moduleId);
        if (binding != null) {
            hook.accept(binding, event);
        }
    }
}
