package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ModuleCapabilityBuilder<C extends ModuleCapabilityContext, S extends ModuleCapabilityState> {
    private final String id;
    private Class<C> contextType;
    private boolean contextTypeConfigured;
    private Predicate<ModuleCandidate> enabledWhen = candidate -> true;
    private Function<String, S> stateFactory;
    private BiFunction<String, S, C> contextFactory;

    private ModuleCapabilityEventHook<ModuleRegisterEvent.Pre> registerPreHook = ModuleCapabilityEventHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleRegisterEvent.Post> registerPostHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Pre> loadPreHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Post> loadPostHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Failed> loadFailedHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Pre> enablePreHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Post> enablePostHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Skipped> enableSkippedHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Failed> enableFailedHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Pre> disablePreHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Post> disablePostHook = ModuleCapabilityBindingHook.noop();
    private ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Failed> disableFailedHook = ModuleCapabilityBindingHook.noop();

    ModuleCapabilityBuilder(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        this.id = id.trim();
    }

    public ModuleCapabilityBuilder<C, S> contextType(Class<C> contextType) {
        if (contextType == null) {
            throw new IllegalArgumentException("contextType cannot be null");
        }
        this.contextType = contextType;
        this.contextTypeConfigured = true;
        return this;
    }

    public ModuleCapabilityBuilder<C, S> enabledWhen(Predicate<ModuleCandidate> enabledWhen) {
        if (enabledWhen == null) {
            throw new IllegalArgumentException("enabledWhen cannot be null");
        }
        this.enabledWhen = enabledWhen;
        return this;
    }

    public ModuleCapabilityBuilder<C, S> stateFactory(Function<String, S> stateFactory) {
        if (stateFactory == null) {
            throw new IllegalArgumentException("stateFactory cannot be null");
        }
        this.stateFactory = stateFactory;
        return this;
    }

    public ModuleCapabilityBuilder<C, S> contextFactory(BiFunction<String, S, C> contextFactory) {
        if (contextFactory == null) {
            throw new IllegalArgumentException("contextFactory cannot be null");
        }
        this.contextFactory = contextFactory;
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onRegisterPre(ModuleCapabilityEventHook<ModuleRegisterEvent.Pre> hook) {
        if (hook == null) {
            throw new IllegalArgumentException("hook cannot be null");
        }
        this.registerPreHook = hook;
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onRegisterPost(ModuleCapabilityBindingHook<C, S, ModuleRegisterEvent.Post> hook) {
        this.registerPostHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onLoadPre(ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Pre> hook) {
        this.loadPreHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onLoadPost(ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Post> hook) {
        this.loadPostHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onLoadFailed(ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Failed> hook) {
        this.loadFailedHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onEnablePre(ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Pre> hook) {
        this.enablePreHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onEnablePost(ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Post> hook) {
        this.enablePostHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onEnableSkipped(ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Skipped> hook) {
        this.enableSkippedHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onEnableFailed(ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Failed> hook) {
        this.enableFailedHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onDisablePre(ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Pre> hook) {
        this.disablePreHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onDisablePost(ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Post> hook) {
        this.disablePostHook = requireHook(hook);
        return this;
    }

    public ModuleCapabilityBuilder<C, S> onDisableFailed(ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Failed> hook) {
        this.disableFailedHook = requireHook(hook);
        return this;
    }

    public ModuleCapability<C, S> build() {
        if (contextTypeConfigured && contextFactory == null) {
            throw new IllegalStateException("contextFactory must be configured when contextType is configured");
        }
        return new ModuleCapability<>(this);
    }

    String id() {
        return id;
    }

    Class<C> contextType() {
        if (contextType != null) {
            return contextType;
        }
        @SuppressWarnings("unchecked")
        Class<C> emptyContextType = (Class<C>) EmptyModuleCapabilityContext.class;
        return emptyContextType;
    }

    boolean contextExposed() {
        return contextTypeConfigured;
    }

    Predicate<ModuleCandidate> enabledWhen() {
        return enabledWhen;
    }

    Function<String, S> stateFactory() {
        if (stateFactory != null) {
            return stateFactory;
        }
        return moduleId -> {
            @SuppressWarnings("unchecked")
            S state = (S) EmptyModuleCapabilityState.INSTANCE;
            return state;
        };
    }

    BiFunction<String, S, C> contextFactory() {
        if (contextFactory != null) {
            return contextFactory;
        }
        return (moduleId, state) -> {
            @SuppressWarnings("unchecked")
            C context = (C) new EmptyModuleCapabilityContext(moduleId);
            return context;
        };
    }

    ModuleCapabilityEventHook<ModuleRegisterEvent.Pre> registerPreHook() {
        return registerPreHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleRegisterEvent.Post> registerPostHook() {
        return registerPostHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Pre> loadPreHook() {
        return loadPreHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Post> loadPostHook() {
        return loadPostHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleLoadEvent.Failed> loadFailedHook() {
        return loadFailedHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Pre> enablePreHook() {
        return enablePreHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Post> enablePostHook() {
        return enablePostHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Skipped> enableSkippedHook() {
        return enableSkippedHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleEnableEvent.Failed> enableFailedHook() {
        return enableFailedHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Pre> disablePreHook() {
        return disablePreHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Post> disablePostHook() {
        return disablePostHook;
    }

    ModuleCapabilityBindingHook<C, S, ModuleDisableEvent.Failed> disableFailedHook() {
        return disableFailedHook;
    }

    private <E extends io.fntlv.bluematrix.core.event.ModuleEvent>
    ModuleCapabilityBindingHook<C, S, E> requireHook(ModuleCapabilityBindingHook<C, S, E> hook) {
        if (hook == null) {
            throw new IllegalArgumentException("hook cannot be null");
        }
        return hook;
    }
}
