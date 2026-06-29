package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryFailure;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleReflectionsFactory;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleRegistrationException;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import io.fntlv.bluematrix.core.module.registration.issue.issues.DuplicateModuleIdIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.InstantiationFailedIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.MissingRequiredDependencyIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.RuntimeLibraryLoadFailedIssue;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.registration.scanned.ScanPackageMarker;
import io.fntlv.bluematrix.core.registration.scanned.ScanPackageType;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import io.fntlv.bluematrix.logging.backend.BlueLogBackendProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModuleRegistrarTest {
    private final BlueLogBackendProvider previousProvider = BlueLoggerFactory.getBackendProvider();

    @AfterEach
    void resetLoggerFactory() {
        BlueLoggerFactory.setBackendProvider(previousProvider);
    }

    @Test
    void registerMergesModuleProviders() {
        ModuleRegistrar registrar = registrar(Arrays.asList(
                new StaticProvider(FirstModule.class),
                new StaticProvider(SecondModule.class)
        ));

        List<ModuleContext> contexts = registrar.register().contexts();

        assertEquals(2, contexts.size());
        assertEquals("first", contexts.get(0).id());
        assertEquals("second", contexts.get(1).id());
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

        List<ModuleContext> contexts = registrar.register().contexts();

        assertTrue(contexts.isEmpty());
        assertEquals(0, provider.instantiateCount);
    }

    @Test
    void duplicateModuleIdsAreReturnedAsIssues() {
        StaticProvider provider = new StaticProvider(FirstDuplicateModule.class, SecondDuplicateModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                provider
        );

        ModuleRegistrationResult result = registrar.register();

        assertTrue(result.contexts().isEmpty());
        assertEquals(2, result.issues().size());
        DuplicateModuleIdIssue firstIssue = assertInstanceOf(DuplicateModuleIdIssue.class, result.issues().all().get(0));
        DuplicateModuleIdIssue secondIssue = assertInstanceOf(DuplicateModuleIdIssue.class, result.issues().all().get(1));
        assertIssue(firstIssue, ModuleRegistrationIssueType.DUPLICATE_MODULE_ID, "duplicate");
        assertIssue(secondIssue, ModuleRegistrationIssueType.DUPLICATE_MODULE_ID, "duplicate");
        assertEquals("duplicate", firstIssue.duplicatedModuleId());
        assertTrue(firstIssue.moduleClassName().contains("DuplicateModule"));
        assertEquals(0, provider.instantiateCount);
    }

    @Test
    void uniqueModuleIdsRemainAvailable() {
        ModuleRegistrar registrar = registrar(Collections.singletonList(
                new StaticProvider(FirstModule.class)
        ));

        List<ModuleContext> contexts = registrar.register().contexts();

        assertEquals(1, contexts.size());
        assertEquals("first", contexts.get(0).id());
    }

    @Test
    void registerReturnsContextsAndIssues() {
        ModuleRegistrar registrar = registrar(Arrays.asList(
                new StaticProvider(FirstModule.class),
                new StaticProvider(SecondModule.class)
        ));

        ModuleRegistrationResult result = registrar.register();

        assertEquals(Arrays.asList("first", "second"), contextIds(result.contexts()));
        assertTrue(result.issues().isEmpty());
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

        List<ModuleContext> contexts = registrar.register().contexts();

        assertEquals(1, contexts.size());
        assertEquals("first", contexts.get(0).id());
    }

    @Test
    void instantiationErrorIsReturnedAsIssueAndContinues() {
        StaticProvider provider = new StaticProvider(FailingInstantiateModule.class, FirstModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                provider
        );

        ModuleRegistrationResult result = registrar.register();

        assertEquals(Arrays.asList("first"), contextIds(result.contexts()));
        assertEquals(1, result.issues().size());
        InstantiationFailedIssue issue = assertInstanceOf(InstantiationFailedIssue.class, result.issues().all().get(0));
        assertIssue(issue, ModuleRegistrationIssueType.INSTANTIATION_FAILED, "failing-instantiate");
        assertTrue(issue.cause() instanceof ModuleInstantiationException);
    }

    @Test
    void discoveryErrorThrowsRegistrationException() {
        ModuleRegistrar registrar = registrar(Arrays.asList(
                new FailingDiscoveryProvider(),
                new StaticProvider(FirstModule.class)
        ));

        ModuleRegistrationException exception = assertThrows(ModuleRegistrationException.class, registrar::register);
        assertTrue(exception.getCause() instanceof ModuleDiscoveryException);
    }

    @Test
    void dependencyResolutionIssuesAreReturned() {
        ModuleRegistrar registrar = registrar(Collections.singletonList(
                new StaticProvider(DependsOnMissingModule.class, FirstModule.class)
        ));

        ModuleRegistrationResult result = registrar.register();

        assertEquals(Arrays.asList("first"), contextIds(result.contexts()));
        assertEquals(1, result.issues().size());
        MissingRequiredDependencyIssue issue = assertInstanceOf(MissingRequiredDependencyIssue.class, result.issues().all().get(0));
        assertIssue(issue, ModuleRegistrationIssueType.MISSING_REQUIRED_DEPENDENCY, "depends-on-missing");
        assertEquals(Collections.singletonList("missing"), issue.missingDependencyIds());
    }

    @Test
    void discoveryIssuesAreMergedIntoRegistrationResult() {
        ModuleRegistrar registrar = registrar(Collections.singletonList(
                new LibraryIssueProvider()
        ));

        ModuleRegistrationResult result = registrar.register();

        assertTrue(result.contexts().isEmpty());
        assertEquals(1, result.issues().size());
        RuntimeLibraryLoadFailedIssue issue = assertInstanceOf(
                RuntimeLibraryLoadFailedIssue.class,
                result.issues().all().get(0)
        );
        assertIssue(issue, ModuleRegistrationIssueType.RUNTIME_LIBRARY_LOAD_FAILED, "library-issue");
        assertEquals(Collections.singletonList("com.example:missing:1.0.0"), issue.failedLibraries());
    }

    @Test
    void discoveryRuntimeErrorThrowsRegistrationException() {
        ModuleRegistrar registrar = registrar(Collections.singletonList(
                new RuntimeFailingDiscoveryProvider()
        ));

        assertThrows(ModuleRegistrationException.class, registrar::register);
    }

    @Test
    void instantiationRuntimeErrorThrowsRegistrationException() {
        StaticProvider provider = new StaticProvider(FirstModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(Collections.singletonList(
                provider
        ), new TopologyDependencyResolver(), new DefaultModuleEventBus(), new RuntimeFailingInstanceFactory());

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

        List<ModuleContext> contexts = registrar.register().contexts();

        assertEquals(1, contexts.size());
        assertEquals(1, listener.preCount);
        assertEquals(1, listener.postCount);
        assertEquals("first", listener.moduleId);
        assertEquals(0, listener.instantiateCountWhenPrePublished);
        assertEquals(1, listener.instantiateCountWhenPostPublished);
        assertNotNull(contexts.get(0).getReflections());
    }

    @Test
    void registerLogsFinalResultAfterSummary() {
        RecordingBackend backend = new RecordingBackend();
        BlueLoggerFactory.setBackendProvider(name -> backend);
        StaticProvider provider = new StaticProvider(FailingInstantiateModule.class, FirstModule.class);
        ModuleRegistrar registrar = new DefaultModuleRegistrar(
                Collections.singletonList(provider),
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                provider
        );

        registrar.register();

        int summary = backend.indexOf("Module register completed.");
        int registeredHeader = backend.indexOf("Registered modules:");
        int registeredModule = backend.indexOf(" - First (first) -");
        int issueHeader = backend.indexOf("Registration issues:");
        int issue = backend.indexOf(" - INSTANTIATION_FAILED | Failing Instantiate (failing-instantiate) -");

        assertTrue(summary >= 0);
        assertTrue(registeredHeader > summary);
        assertTrue(registeredModule > registeredHeader);
        assertTrue(issueHeader > registeredModule);
        assertTrue(issue > issueHeader);
    }

    @Test
    void moduleContextCreatesReflectionsFromCandidateDescriptor() {
        FirstModule module = new FirstModule();
        ModuleCandidate candidate = new ModuleCandidate(
                FirstModule.class,
                FirstModule.class.getAnnotation(ModuleInfo.class)
        );

        ModuleContext context = new ModuleContext(module, candidate);

        assertNotNull(context.getReflections());
    }

    @Test
    void moduleReflectionsFactoryUsesConfiguredScanPackages() {
        ModuleCandidate candidate = new ModuleCandidate(
                ScanPackageModule.class,
                ScanPackageModule.class.getAnnotation(ModuleInfo.class)
        );

        assertTrue(ModuleReflectionsFactory.create(candidate.getModuleClass(), candidate.getDescriptor())
                .getSubTypesOf(ScanPackageType.class)
                .contains(ScanPackageMarker.class));
    }

    private static List<String> contextIds(List<ModuleContext> contexts) {
        return contexts.stream()
                .map(context -> context.id())
                .collect(java.util.stream.Collectors.toList());
    }

    private static ModuleRegistrar registrar(List<ModuleProvider> providers) {
        return new DefaultModuleRegistrar(
                providers,
                new TopologyDependencyResolver(),
                new DefaultModuleEventBus(),
                new StaticProvider()
        );
    }

    private static void assertIssue(ModuleRegistrationIssue issue,
                                    ModuleRegistrationIssueType type,
                                    String moduleId) {
        assertEquals(type, issue.type());
        assertEquals(moduleId, issue.moduleId());
        assertTrue(issue.message().length() > 0);
    }

    private static class StaticProvider implements ModuleProvider, ModuleInstanceFactory {
        private final List<Class<? extends Module>> moduleClasses;
        private int instantiateCount;

        @SafeVarargs
        private StaticProvider(Class<? extends Module>... moduleClasses) {
            this.moduleClasses = Arrays.asList(moduleClasses);
        }

        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            return ModuleRegistrationStageResult.of(moduleClasses.stream()
                    .map(moduleClass -> new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class)))
                    .collect(java.util.stream.Collectors.toList()));
        }

        @Override
        public Module create(ModuleCandidate moduleCandidate) {
            return createModule(moduleCandidate);
        }

        @Override
        public Module createModule(ModuleCandidate moduleCandidate) {
            instantiateCount++;
            if (moduleCandidate.getModuleClass().equals(FailingInstantiateModule.class)) {
                throw new ModuleInstantiationException(moduleCandidate.id(),
                        new IllegalStateException("Expected instantiate failure"));
            }
            try {
                Constructor<? extends Module> constructor = moduleCandidate.getModuleClass().getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new ModuleInstantiationException(moduleCandidate.id(), e);
            }
        }

        @Override
        public <T> T createOther(Class<T> type, OtherInjectionContext context) {
            throw new UnsupportedOperationException("StaticProvider only creates module instances");
        }
    }

    private static class RuntimeFailingInstanceFactory implements ModuleInstanceFactory {
        @Override
        public Module create(ModuleCandidate candidate) {
            return createModule(candidate);
        }

        @Override
        public Module createModule(ModuleCandidate candidate) {
            throw new IllegalStateException("Expected instantiation runtime failure");
        }

        @Override
        public <T> T createOther(Class<T> type, OtherInjectionContext context) {
            throw new UnsupportedOperationException("RuntimeFailingInstanceFactory only creates module instances");
        }
    }

    private static class FailingDiscoveryProvider implements ModuleProvider {
        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            throw new ModuleDiscoveryException("Expected discovery failure");
        }
    }

    private static class RuntimeFailingDiscoveryProvider implements ModuleProvider {
        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            throw new IllegalStateException("Expected discovery runtime failure");
        }
    }

    private static class LibraryIssueProvider implements ModuleProvider {
        @Override
        public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
            ModuleRuntimeLibraryLoadResult result = ModuleRuntimeLibraryLoadResult.of(
                    "library-issue",
                    Collections.singletonList(new ModuleRuntimeLibraryFailure(
                            "com.example:missing:1.0.0",
                            new IllegalStateException("download failed")
                    ))
            );
            RuntimeLibraryLoadFailedIssue issue = new RuntimeLibraryLoadFailedIssue(
                    "library-issue",
                    "Library Issue",
                    LibraryIssueModule.class.getName(),
                    result,
                    "Failed to load runtime libraries: com.example:missing:1.0.0"
            );
            return ModuleRegistrationStageResult.of(
                    Collections.emptyList(),
                    new io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues(
                            Collections.singletonList(issue)
                    )
            );
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
            moduleId = event.getContext().id();
            instantiateCountWhenPostPublished = provider.instantiateCount;
        }
    }

    private static class RecordingBackend implements BlueLogBackend {
        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean isEnabled(BlueLogLevel level) {
            return true;
        }

        @Override
        public void log(BlueLogLevel level, String message) {
            messages.add(message);
        }

        @Override
        public void log(BlueLogLevel level, String message, Throwable throwable) {
            messages.add(message);
        }

        private int indexOf(String text) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).contains(text)) {
                    return i;
                }
            }
            return -1;
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

    @ModuleInfo(id = "depends-on-missing", name = "Depends on Missing", dependencies = "missing")
    private static class DependsOnMissingModule implements Module {
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

    @ModuleInfo(id = "library-issue", name = "Library Issue")
    private static class LibraryIssueModule implements Module {
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
