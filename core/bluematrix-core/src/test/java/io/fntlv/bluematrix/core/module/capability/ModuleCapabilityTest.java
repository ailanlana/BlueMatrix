package io.fntlv.bluematrix.core.module.capability;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleCapabilityTest {
    @Test
    void capabilityRegistersContextStateAndBindingWhenEnabled() {
        ModuleCapability<TestContext, TestState> capability = testCapability(true).build();
        ModuleCandidate candidate = candidate(TestModule.class);

        capability.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        ModuleCapabilityBinding<TestContext, TestState> binding = capability.binding(candidate.id());
        assertTrue(capability.contains(candidate.id()));
        assertEquals(candidate.id(), binding.moduleId());
        assertEquals(candidate.id(), binding.context().moduleId());
        assertSame(binding.state(), capability.getState(candidate.id()));
        assertSame(binding.context(), capability.context(candidate.id()));
    }

    @Test
    void capabilitySkipsDisabledModule() {
        ModuleCapability<TestContext, TestState> capability = testCapability(false).build();
        ModuleCandidate candidate = candidate(TestModule.class);

        capability.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertFalse(capability.contains(candidate.id()));
        assertThrows(IllegalStateException.class, () -> capability.context(candidate.id()));
    }

    @Test
    void registryFindsCapabilityByContextTypeAndRejectsDuplicateCapabilityId() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<TestContext, TestState> capability = testCapability(true).build();
        registry.register(capability);

        List<ModuleCapability<?, ?>> matches = registry.findByContextType(TestContext.class);

        assertEquals(1, matches.size());
        assertSame(capability, matches.get(0));
        assertThrows(IllegalArgumentException.class, () -> registry.register(testCapability(true).build()));
    }

    @Test
    void listenerDispatchesAllLifecycleHooksToRegisteredCapability() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<TestContext, TestState> capability = testCapability(true)
                .onRegisterPre(event -> {
                })
                .onRegisterPost((binding, event) -> binding.state().registerPostCalls++)
                .onLoadPre((binding, event) -> binding.state().loadPreCalls++)
                .onLoadPost((binding, event) -> binding.state().loadPostCalls++)
                .onLoadFailed((binding, event) -> binding.state().loadFailedCalls++)
                .onEnablePre((binding, event) -> binding.state().enablePreCalls++)
                .onEnablePost((binding, event) -> binding.state().enablePostCalls++)
                .onEnableSkipped((binding, event) -> binding.state().enableSkippedCalls++)
                .onEnableFailed((binding, event) -> binding.state().enableFailedCalls++)
                .onDisablePre((binding, event) -> binding.state().disablePreCalls++)
                .onDisablePost((binding, event) -> binding.state().disablePostCalls++)
                .build();
        registry.register(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(TestModule.class);
        ModuleContext context = context(candidate);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onRegisterPost(new ModuleRegisterEvent.Post(context));
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));
        listener.onLoadPost(new ModuleLoadEvent.Post(context));
        listener.onLoadFailed(new ModuleLoadEvent.Failed(context, new IllegalStateException("load")));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        listener.onEnablePost(new ModuleEnableEvent.Post(context));
        listener.onEnableSkipped(new ModuleEnableEvent.Skipped(
                context,
                ModuleConditionOutcome.noMatch("test", "skip")
        ));
        listener.onEnableFailed(new ModuleEnableEvent.Failed(context, new IllegalStateException("enable")));
        listener.onDisablePre(new ModuleDisableEvent.Pre(context));
        TestState state = capability.getState(context.id());
        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        assertEquals(1, state.registerPostCalls);
        assertEquals(1, state.loadPreCalls);
        assertEquals(1, state.loadPostCalls);
        assertEquals(1, state.loadFailedCalls);
        assertEquals(1, state.enablePreCalls);
        assertEquals(1, state.enablePostCalls);
        assertEquals(1, state.enableSkippedCalls);
        assertEquals(1, state.enableFailedCalls);
        assertEquals(1, state.disablePreCalls);
        assertEquals(1, state.disablePostCalls);
        assertFalse(capability.contains(context.id()));
    }

    @Test
    void listenerWritesLoadAndEnableErrors() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        registry.register(testCapability(true)
                .onLoadPre((binding, event) -> {
                    throw new IllegalStateException("load failed");
                })
                .onEnablePre((binding, event) -> {
                    throw new IllegalStateException("enable failed");
                })
                .build());
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(TestModule.class);
        ModuleContext context = context(candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModuleLoadEvent.Pre load = new ModuleLoadEvent.Pre(context);
        ModuleEnableEvent.Pre enable = new ModuleEnableEvent.Pre(context);

        listener.onLoadPre(load);
        listener.onEnablePre(enable);

        assertTrue(load.hasError());
        assertEquals("test", load.getErrorSource());
        assertTrue(enable.hasError());
        assertEquals("test", enable.getErrorSource());
    }

    @Test
    void contextResolverResolvesRegisteredCapabilityContext() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<TestContext, TestState> capability = registry.register(testCapability(true).build());
        ModuleCandidate candidate = candidate(TestModule.class);
        capability.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        Object resolved = resolver.resolve(TestContext.class, ModuleInjectionContext.from(candidate));

        assertTrue(resolver.supports(TestContext.class, ModuleInjectionContext.from(candidate)));
        assertFalse(resolver.supports(Object.class, ModuleInjectionContext.from(candidate)));
        assertSame(capability.context(candidate.id()), resolved);
    }

    @Test
    void contextResolverFailsWhenCapabilityIsNotRegisteredForModule() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        registry.register(testCapability(false).build());
        ModuleCandidate candidate = candidate(TestModule.class);
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        assertTrue(resolver.supports(TestContext.class, ModuleInjectionContext.from(candidate)));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(TestContext.class, ModuleInjectionContext.from(candidate)));
    }

    @Test
    void stateCapabilityCreatesStateWithoutExposingEmptyContext() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<EmptyModuleCapabilityContext, TestState> capability = registry.register(
                ModuleCapability.<EmptyModuleCapabilityContext, TestState>builder("state-only")
                        .stateFactory(moduleId -> new TestState())
                        .onEnablePre((binding, event) -> binding.state().enablePreCalls++)
                        .build()
        );
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(TestModule.class);
        ModuleContext context = context(candidate);
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertEquals(1, capability.getState(candidate.id()).enablePreCalls);
        assertFalse(resolver.supports(EmptyModuleCapabilityContext.class, ModuleInjectionContext.from(candidate)));
    }

    @Test
    void defaultCapabilityRunsHooksWithoutExposingEmptyContext() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<EmptyModuleCapabilityContext, EmptyModuleCapabilityState> capability = registry.register(
                ModuleCapability.<EmptyModuleCapabilityContext, EmptyModuleCapabilityState>builder("lifecycle-only")
                        .onLoadPre((binding, event) -> event.getContext().markLoaded())
                        .build()
        );
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(TestModule.class);
        ModuleContext context = context(candidate);
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        assertTrue(context.isEnabled() || context.canEnable());
        assertFalse(resolver.supports(EmptyModuleCapabilityContext.class, ModuleInjectionContext.from(candidate)));
        assertSame(EmptyModuleCapabilityState.INSTANCE, capability.getState(candidate.id()));
    }

    @Test
    void contextFactoryIsRequiredWhenContextTypeIsConfigured() {
        assertThrows(IllegalStateException.class, () -> ModuleCapability.<TestContext, TestState>builder("test")
                .contextType(TestContext.class)
                .stateFactory(moduleId -> new TestState())
                .build());
    }

    @Test
    void explicitContextTypeIsExposedToResolver() {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        ModuleCapability<TestContext, TestState> capability = registry.register(testCapability(true).build());
        ModuleCandidate candidate = candidate(TestModule.class);
        capability.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        assertTrue(resolver.supports(TestContext.class, ModuleInjectionContext.from(candidate)));
        assertSame(capability.context(candidate.id()),
                resolver.resolve(TestContext.class, ModuleInjectionContext.from(candidate)));
    }

    private static ModuleCapabilityBuilder<TestContext, TestState> testCapability(boolean enabled) {
        return ModuleCapability.<TestContext, TestState>builder("test")
                .contextType(TestContext.class)
                .enabledWhen(candidate -> enabled)
                .stateFactory(moduleId -> new TestState())
                .contextFactory((moduleId, state) -> new TestContext(moduleId));
    }

    private static ModuleCandidate candidate(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static ModuleContext context(ModuleCandidate candidate) {
        return new ModuleContext(new TestModule(), candidate);
    }

    private static final class TestContext implements ModuleCapabilityContext {
        private final String moduleId;

        private TestContext(String moduleId) {
            this.moduleId = moduleId;
        }

        @Override
        public String moduleId() {
            return moduleId;
        }
    }

    private static final class TestState implements ModuleCapabilityState {
        private int registerPostCalls;
        private int loadPreCalls;
        private int loadPostCalls;
        private int loadFailedCalls;
        private int enablePreCalls;
        private int enablePostCalls;
        private int enableSkippedCalls;
        private int enableFailedCalls;
        private int disablePreCalls;
        private int disablePostCalls;
    }

    @ModuleInfo(id = "test-module", name = "Test Module")
    private static final class TestModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
