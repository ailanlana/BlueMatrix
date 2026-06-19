package io.fntlv.bluematrix.core.module.orchestration;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.DefaultModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                new DefaultModuleOrchestrator(moduleStore, moduleRegistrar, eventBus),
                moduleRegistry,
                eventBus
        );
    }

    private static class ModuleRuntime {
        private final ModuleOrchestrator orchestrator;
        private final ModuleRegistry registry;
        private final ModuleEventBus eventBus;

        private ModuleRuntime(ModuleOrchestrator orchestrator, ModuleRegistry registry, ModuleEventBus eventBus) {
            this.orchestrator = orchestrator;
            this.registry = registry;
            this.eventBus = eventBus;
        }
    }

    private static class FakeProvider implements ModuleProvider {
        @Override
        public List<ModuleCandidate> discoverModules() {
            return Arrays.asList(
                    provided(FirstDuplicateModule.class),
                    provided(SecondDuplicateModule.class)
            );
        }
    }

    private static class FailingThenWorkingProvider implements ModuleProvider {
        @Override
        public List<ModuleCandidate> discoverModules() {
            return Arrays.asList(
                    provided(FailingModule.class),
                    provided(WorkingModule.class)
            );
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
                throw new ModuleInstantiationException(moduleCandidate.getModuleInfo().id(),
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
}
