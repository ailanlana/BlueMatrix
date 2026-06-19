package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleRegistrationException;
import io.fntlv.bluematrix.core.module.registration.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.registration.scanned.ScanPackageMarker;
import io.fntlv.bluematrix.core.registration.scanned.ScanPackageType;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.reflections.Reflections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModuleRegistrarTest {

    @Test
    void registerMergesModuleProviders() {
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Arrays.asList(
                new StaticProvider(FirstModule.class),
                new StaticProvider(SecondModule.class)
        ));

        List<ModuleContext> contexts = registrar.register();

        assertEquals(2, contexts.size());
        assertEquals("first", contexts.get(0).getInfo().id());
        assertEquals("second", contexts.get(1).getInfo().id());
    }

    @Test
    void duplicateModuleIdsAreReturnedAsFailures() {
        StaticProvider provider = new StaticProvider(FirstDuplicateModule.class, SecondDuplicateModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                provider
        );

        List<ModuleContext> contexts = registrar.register();

        assertTrue(contexts.isEmpty());
        assertEquals(0, provider.instantiateCount);
    }

    @Test
    void uniqueModuleIdsRemainAvailable() {
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Collections.singletonList(
                new StaticProvider(FirstModule.class)
        ));

        List<ModuleContext> contexts = registrar.register();

        assertEquals(1, contexts.size());
        assertEquals("first", contexts.get(0).getInfo().id());
    }

    @Test
    void instantiateErrorReturnsEntryAndContinues() {
        StaticProvider provider = new StaticProvider(FailingInstantiateModule.class, FirstModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                provider
        );

        List<ModuleContext> contexts = registrar.register();

        assertEquals(1, contexts.size());
        assertEquals("first", contexts.get(0).getInfo().id());
    }

    @Test
    void discoverErrorSkipsProviderAndContinues() {
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Arrays.asList(
                new FailingDiscoveryProvider(),
                new StaticProvider(FirstModule.class)
        ));

        List<ModuleContext> contexts = registrar.register();

        assertEquals(1, contexts.size());
        assertEquals("first", contexts.get(0).getInfo().id());
    }

    @Test
    void discoveryRuntimeErrorThrowsRegistrationException() {
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Collections.singletonList(
                new RuntimeFailingDiscoveryProvider()
        ));

        assertThrows(ModuleRegistrationException.class, registrar::register);
    }

    @Test
    void instantiationRuntimeErrorThrowsRegistrationException() {
        StaticProvider provider = new StaticProvider(FirstModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Collections.singletonList(
                provider
        ), new TopologyDependencyResolver(), new DefaultModuleEventBus(), candidate -> {
            throw new IllegalStateException("Expected instantiation runtime failure");
        });

        assertThrows(ModuleRegistrationException.class, registrar::register);
    }

    @Test
    void registerPublishesPreBeforeInstantiationAndPostAfterContextCreation() {
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        RegisterCountingListener listener = new RegisterCountingListener();
        StaticProvider provider = new StaticProvider(FirstModule.class);
        listener.provider = provider;
        eventBus.registerListener(listener);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                eventBus,
                provider
        );

        List<ModuleContext> contexts = registrar.register();

        assertEquals(1, contexts.size());
        assertEquals(1, listener.preCount);
        assertEquals(1, listener.postCount);
        assertEquals("first", listener.moduleId);
        assertEquals(0, listener.instantiateCountWhenPrePublished);
        assertEquals(1, listener.instantiateCountWhenPostPublished);
        assertSame(listener.preCandidate.getReflections(), contexts.get(0).getReflections());
    }

    @Test
    void moduleContextUsesCandidateReflections() {
        FirstModule module = new FirstModule();
        Reflections reflections = new Reflections(FirstModule.class.getPackage().getName());
        ModuleCandidate candidate = new ModuleCandidate(
                FirstModule.class,
                FirstModule.class.getAnnotation(ModuleInfo.class),
                reflections
        );

        ModuleContext context = new ModuleContext(module, candidate);

        assertSame(reflections, context.getReflections());
    }

    @Test
    void candidateUsesConfiguredScanPackages() {
        ModuleCandidate candidate = new ModuleCandidate(
                ScanPackageModule.class,
                ScanPackageModule.class.getAnnotation(ModuleInfo.class)
        );

        assertTrue(candidate.getReflections().getSubTypesOf(ScanPackageType.class).contains(ScanPackageMarker.class));
    }

    private static class StaticProvider implements ModuleProvider, ModuleInstanceFactory {
        private final List<Class<? extends Module>> moduleClasses;
        private int instantiateCount;

        @SafeVarargs
        private StaticProvider(Class<? extends Module>... moduleClasses) {
            this.moduleClasses = Arrays.asList(moduleClasses);
        }

        @Override
        public List<ModuleCandidate> discoverModules() {
            return moduleClasses.stream()
                    .map(moduleClass -> new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class)))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public Module create(ModuleCandidate moduleCandidate) {
            instantiateCount++;
            if (moduleCandidate.getModuleClass().equals(FailingInstantiateModule.class)) {
                throw new ModuleInstantiationException(moduleCandidate.getModuleInfo().id(),
                        new IllegalStateException("Expected instantiate failure"));
            }
            try {
                Constructor<? extends Module> constructor = moduleCandidate.getModuleClass().getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new ModuleInstantiationException(moduleCandidate.getModuleInfo().id(), e);
            }
        }
    }

    private static class FailingDiscoveryProvider implements ModuleProvider {
        @Override
        public List<ModuleCandidate> discoverModules() {
            throw new ModuleDiscoveryException("Expected discovery failure");
        }
    }

    private static class RuntimeFailingDiscoveryProvider implements ModuleProvider {
        @Override
        public List<ModuleCandidate> discoverModules() {
            throw new IllegalStateException("Expected discovery runtime failure");
        }
    }

    private static class RegisterCountingListener {
        private StaticProvider provider;
        private int preCount;
        private int postCount;
        private String moduleId;
        private int instantiateCountWhenPrePublished;
        private int instantiateCountWhenPostPublished;
        private ModuleCandidate preCandidate;

        @ModuleEventListener
        public void onRegisterPre(ModuleRegisterEvent.Pre event) {
            preCount++;
            preCandidate = event.getCandidate();
            instantiateCountWhenPrePublished = provider.instantiateCount;
        }

        @ModuleEventListener
        public void onRegisterPost(ModuleRegisterEvent.Post event) {
            postCount++;
            moduleId = event.getContext().getInfo().id();
            instantiateCountWhenPostPublished = provider.instantiateCount;
        }
    }

    @ModuleInfo(id = "first", name = "First")
    private static class FirstModule implements Module {
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

    @ModuleInfo(id = "second", name = "Second")
    private static class SecondModule implements Module {
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

    @ModuleInfo(id = "failing-instantiate", name = "Failing Instantiate")
    private static class FailingInstantiateModule implements Module {
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

    @ModuleInfo(
            id = "scan-package",
            name = "Scan Package",
            scanPackages = "io.fntlv.bluematrix.core.registration.scanned"
    )
    private static class ScanPackageModule implements Module {
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
