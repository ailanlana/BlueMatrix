package io.fntlv.bluematrix.core.module.orchestration;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.LifecycleManager;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.DefaultModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationResult;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModuleOrchestratorTest {

    @TempDir
    File tempDir;

    @Test
    void duplicateModuleIdsAreSkipped() {
        FakeProvider provider = new FakeProvider();
        TestInstanceFactory factory = new TestInstanceFactory();
        ModuleOrchestrator kernel = runtime(Arrays.asList(provider), factory).orchestrator;

        kernel.initialize();

        assertEquals(0, factory.instantiateCount);
    }

    @Test
    void registryIsNotModuleOrchestrator() {
        ModuleRegistry registry = runtime(Collections.emptyList()).registry;

        assertFalse(registry instanceof ModuleOrchestrator);
        assertFalse(registry instanceof DefaultModuleOrchestrator);
    }

    @Test
    void registerErrorDoesNotStopLaterModules() {
        FailingThenWorkingProvider provider = new FailingThenWorkingProvider();
        TestInstanceFactory factory = new TestInstanceFactory();
        ModuleRuntime runtime = runtime(Arrays.asList(provider), factory);
        ModuleOrchestrator kernel = runtime.orchestrator;

        kernel.initialize();

        assertEquals(2, factory.instantiateCount);
        assertTrue(runtime.registry.getModule(WorkingModule.class).isPresent());
    }

    @Test
    void loadModulesUsesRegistryOrder() {
        ModuleStore moduleStore = new ModuleStore();
        List<ModuleContext> contexts = contexts(FirstOrderModule.class, SecondOrderModule.class);
        moduleStore.add(contexts.get(0));
        moduleStore.add(contexts.get(1));
        RecordingLifecycle lifecycle = new RecordingLifecycle();
        ModuleOrchestrator orchestrator = new DefaultModuleOrchestrator(
                moduleStore,
                StaticModuleRegistrar.empty(),
                lifecycle
        );

        orchestrator.loadModules();

        assertIterableEquals(Arrays.asList("load:first-order", "load:second-order"), lifecycle.calls);
    }

    @Test
    void enableModulesUsesRegistryOrder() {
        ModuleStore moduleStore = new ModuleStore();
        List<ModuleContext> contexts = contexts(FirstOrderModule.class, SecondOrderModule.class);
        moduleStore.add(contexts.get(0));
        moduleStore.add(contexts.get(1));
        RecordingLifecycle lifecycle = new RecordingLifecycle();
        ModuleOrchestrator orchestrator = new DefaultModuleOrchestrator(
                moduleStore,
                StaticModuleRegistrar.empty(),
                lifecycle
        );

        orchestrator.enableModules();

        assertIterableEquals(Arrays.asList("enable:first-order", "enable:second-order"), lifecycle.calls);
    }

    @Test
    void disableModulesUsesReverseRegistryOrder() {
        ModuleStore moduleStore = new ModuleStore();
        List<ModuleContext> contexts = contexts(FirstOrderModule.class, SecondOrderModule.class);
        moduleStore.add(contexts.get(0));
        moduleStore.add(contexts.get(1));
        RecordingLifecycle lifecycle = new RecordingLifecycle();
        ModuleOrchestrator orchestrator = new DefaultModuleOrchestrator(
                moduleStore,
                StaticModuleRegistrar.empty(),
                lifecycle
        );

        orchestrator.disableModules();

        assertIterableEquals(Arrays.asList("disable:second-order", "disable:first-order"), lifecycle.calls);
    }

    @Test
    void initializeRegistersOnlyOnce() {
        ModuleStore moduleStore = new ModuleStore();
        List<ModuleContext> contexts = contexts(FirstOrderModule.class, SecondOrderModule.class);
        StaticModuleRegistrar registrar = new StaticModuleRegistrar(contexts);
        ModuleOrchestrator orchestrator = new DefaultModuleOrchestrator(
                moduleStore,
                registrar,
                new RecordingLifecycle()
        );

        orchestrator.initialize();
        orchestrator.initialize();

        assertEquals(1, registrar.registerCount);
        assertEquals(2, moduleStore.size());
    }

    private ModuleRuntime runtime(List<ModuleProvider> providers) {
        return runtime(providers, new TestInstanceFactory());
    }

    private ModuleRuntime runtime(List<ModuleProvider> providers, ModuleInstanceFactory instanceFactory) {
        ModuleStore moduleStore = new ModuleStore();
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(moduleStore, tempDir);
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        DefaultModuleRegistrar moduleRegistrar = new DefaultModuleRegistrar(
                providers,
                new TopologyDependencyResolver(),
                eventBus,
                instanceFactory
        );
        return new ModuleRuntime(
                new DefaultModuleOrchestrator(moduleStore, moduleRegistrar, new RecordingLifecycle()),
                moduleRegistry
        );
    }

    private static class ModuleRuntime {
        private final ModuleOrchestrator orchestrator;
        private final ModuleRegistry registry;

        private ModuleRuntime(ModuleOrchestrator orchestrator, ModuleRegistry registry) {
            this.orchestrator = orchestrator;
            this.registry = registry;
        }
    }

    private static class FakeProvider implements ModuleProvider {
        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            return ModuleRegistrationStageResult.of(Arrays.asList(
                    provided(FirstDuplicateModule.class),
                    provided(SecondDuplicateModule.class)
            ));
        }
    }

    private static class FailingThenWorkingProvider implements ModuleProvider {
        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            return ModuleRegistrationStageResult.of(Arrays.asList(
                    provided(FailingModule.class),
                    provided(WorkingModule.class)
            ));
        }
    }

    private static class TestInstanceFactory implements ModuleInstanceFactory {
        private int instantiateCount;

        @Override
        public Module create(ModuleCandidate moduleCandidate) {
            return createModule(moduleCandidate);
        }

        @Override
        public Module createModule(ModuleCandidate moduleCandidate) {
            instantiateCount++;
            if (moduleCandidate.getModuleClass().equals(FailingModule.class)) {
                throw new ModuleInstantiationException(moduleCandidate.id(),
                        new IllegalStateException("Expected register failure"));
            }
            return new WorkingModule();
        }

        @Override
        public <T> T createOther(Class<T> type, OtherInjectionContext context) {
            throw new UnsupportedOperationException("TestInstanceFactory only creates module instances");
        }
    }

    private static ModuleCandidate provided(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static List<ModuleContext> contexts(Class<? extends Module> first,
                                                Class<? extends Module> second) {
        return Arrays.asList(context(first), context(second));
    }

    private static ModuleContext context(Class<? extends Module> moduleClass) {
        try {
            Module module = moduleClass.getDeclaredConstructor().newInstance();
            return new ModuleContext(module, provided(moduleClass));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test module context", e);
        }
    }

    private static class StaticModuleRegistrar implements ModuleRegistrar {
        private final List<ModuleContext> contexts;
        private int registerCount;

        private StaticModuleRegistrar(List<ModuleContext> contexts) {
            this.contexts = contexts;
        }

        private static StaticModuleRegistrar empty() {
            return new StaticModuleRegistrar(Collections.emptyList());
        }

        @Override
        public ModuleRegistrationResult register() {
            registerCount++;
            return ModuleRegistrationResult.success(contexts);
        }
    }

    private static class RecordingLifecycle implements LifecycleManager {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void loadModule(ModuleContext context) {
            calls.add("load:" + context.id());
        }

        @Override
        public void enableModule(ModuleContext context) {
            calls.add("enable:" + context.id());
        }

        @Override
        public void disableModule(ModuleContext context) {
            calls.add("disable:" + context.id());
        }

        @Override
        public void reloadModule(ModuleContext context) {
            calls.add("reload:" + context.id());
        }
    }

    @ModuleInfo(id = "duplicate", name = "First Duplicate")
    private static class FirstDuplicateModule implements Module {
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

    @ModuleInfo(id = "duplicate", name = "Second Duplicate")
    private static class SecondDuplicateModule implements Module {
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

    @ModuleInfo(id = "failing", name = "Failing")
    private static class FailingModule implements Module {
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

    @ModuleInfo(id = "working", name = "Working")
    private static class WorkingModule implements Module {
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

    @ModuleInfo(id = "first-order", name = "First Order")
    public static class FirstOrderModule implements Module {
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

    @ModuleInfo(id = "second-order", name = "Second Order")
    public static class SecondOrderModule implements Module {
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
