package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleRegistrationException;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.outcome.RegistrationOutcomeClassifier;
import io.fntlv.bluematrix.core.module.registration.outcome.RegistrationOutcomeCollector;
import io.fntlv.bluematrix.core.module.registration.resolver.DependencyResolver;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultModuleRegistrar implements ModuleRegistrar {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(DefaultModuleRegistrar.class);

    private final DependencyResolver dependencyResolver;
    private final List<ModuleProvider> moduleProviders;
    private final ModuleEventBus eventBus;
    private final ModuleInstanceFactory instanceFactory;
    private final RegistrationOutcomeClassifier outcomes = new RegistrationOutcomeClassifier(LOGGER);

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders,
                                  DependencyResolver dependencyResolver,
                                  ModuleEventBus eventBus,
                                  ModuleInstanceFactory instanceFactory) {
        if (moduleProviders == null) {
            throw new IllegalArgumentException("moduleProviders cannot be null");
        }
        if (dependencyResolver == null) {
            throw new IllegalArgumentException("dependencyResolver cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus cannot be null");
        }
        if (instanceFactory == null) {
            throw new IllegalArgumentException("instanceFactory cannot be null");
        }
        this.moduleProviders = Collections.unmodifiableList(new ArrayList<>(moduleProviders));
        this.dependencyResolver = dependencyResolver;
        this.eventBus = eventBus;
        this.instanceFactory = instanceFactory;
    }

    @Override
    public ModuleRegistrationResult register() {
        final long startTime = System.currentTimeMillis();
        LOGGER.infoBanner();

        ModuleRegistrationStageResult<ModuleCandidate> discovered = discoverModules();
        ModuleRegistrationStageResult<ModuleCandidate> available = removeIdConflicts(discovered.passed());
        ModuleRegistrationStageResult<ModuleCandidate> resolved = resolveDependencies(available.passed());
        ModuleRegistrationStageResult<ModuleContext> registered = registerResolvedModules(resolved.passed());
        ModuleRegistrationIssues issues = ModuleRegistrationIssues.merge(
                discovered.issues(),
                available.issues(),
                resolved.issues(),
                registered.issues()
        );

        ModuleRegistrationResult result = new ModuleRegistrationResult(registered.passed(), issues);
        LOGGER.info("Module register completed. Success: {} | Issues: {} | Duration: {}ms",
                registered.passed().size(),
                issues.size(),
                System.currentTimeMillis() - startTime
        );
        outcomes.reporter().registrationResult(result);
        return result;
    }

    private ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
        RegistrationOutcomeCollector<ModuleCandidate> collector = new RegistrationOutcomeCollector<>();
        for (ModuleProvider moduleProvider : moduleProviders) {
            try {
                ModuleRegistrationStageResult<ModuleCandidate> result = moduleProvider.discoverModules();
                collector.passAll(result.passed());
                collector.issues(result.issues());
            } catch (ModuleDiscoveryException e) {
                throw new ModuleRegistrationException(
                        "Failed to discover modules from provider: " + moduleProvider.getClass().getName(),
                        e
                );
            } catch (RuntimeException e) {
                throw new ModuleRegistrationException(
                        "Failed to discover modules from provider: " + moduleProvider.getClass().getName(),
                        e
                );
            }
        }
        return collector.toStageResult();
    }

    private ModuleRegistrationStageResult<ModuleCandidate> resolveDependencies(List<ModuleCandidate> modules) {
        try {
            return dependencyResolver.resolve(modules);
        } catch (RuntimeException e) {
            throw new ModuleRegistrationException("Failed to resolve module dependencies", e);
        }
    }

    private ModuleRegistrationStageResult<ModuleCandidate> removeIdConflicts(List<ModuleCandidate> modules) {
        Set<String> conflicts = findConflictIds(modules);
        RegistrationOutcomeCollector<ModuleCandidate> collector = new RegistrationOutcomeCollector<>();
        for (ModuleCandidate module : modules) {
            if (conflicts.contains(module.id())) {
                collector.issue(outcomes.duplicateModuleId(module));
            } else {
                collector.pass(module);
            }
        }
        return collector.toStageResult();
    }

    private ModuleRegistrationStageResult<ModuleContext> registerResolvedModules(List<ModuleCandidate> modules) {
        RegistrationOutcomeCollector<ModuleContext> collector = new RegistrationOutcomeCollector<>();
        for (ModuleCandidate module : modules) {
            try {
                eventBus.publish(new ModuleRegisterEvent.Pre(module));
                Module instance = instanceFactory.create(module);
                ModuleContext context = new ModuleContext(instance, module);
                eventBus.publish(new ModuleRegisterEvent.Post(context));
                collector.pass(context);
                outcomes.reporter().registerSuccess(module);
            } catch (ModuleInstantiationException e) {
                collector.issue(outcomes.instantiationFailed(module, e));
            } catch (RuntimeException e) {
                throw new ModuleRegistrationException(
                        "Failed to register module: " + module.id(),
                        e
                );
            }
        }
        return collector.toStageResult();
    }

    private Set<String> findConflictIds(List<ModuleCandidate> modules) {
        Map<String, Long> countById = modules.stream()
                .collect(Collectors.groupingBy(module -> module.id(), Collectors.counting()));
        return countById.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
