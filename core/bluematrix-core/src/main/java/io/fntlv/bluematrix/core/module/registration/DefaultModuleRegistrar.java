package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleRegistrationException;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleEventBusParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleInstanceFactoryParameterResolver;
import io.fntlv.bluematrix.core.module.registration.resolver.DependencyResolver;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import io.fntlv.bluematrix.core.module.registration.issue.issues.DuplicateModuleIdIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.InstantiationFailedIssue;

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

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders) {
        this(moduleProviders, new TopologyDependencyResolver(), new DefaultModuleEventBus());
    }

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders, DependencyResolver dependencyResolver) {
        this(moduleProviders, dependencyResolver, new DefaultModuleEventBus());
    }

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders,
                                  DependencyResolver dependencyResolver,
                                  ModuleEventBus eventBus) {
        this(moduleProviders, dependencyResolver, eventBus, createDefaultParameterResolvers(eventBus));
    }

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

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders,
                                  DependencyResolver dependencyResolver,
                                  ModuleEventBus eventBus,
                                  ModuleParameterResolverRegistry parameterResolvers) {
        this(moduleProviders, dependencyResolver, eventBus, createInstanceFactory(parameterResolvers));
    }

    private static ModuleParameterResolverRegistry createDefaultParameterResolvers(ModuleEventBus eventBus) {
        ModuleParameterResolverRegistry parameterResolvers = ModuleParameterResolverRegistry.createDefault();
        parameterResolvers.register(new ModuleEventBusParameterResolver(eventBus));
        return parameterResolvers;
    }

    private static ModuleInstanceFactory createInstanceFactory(ModuleParameterResolverRegistry parameterResolvers) {
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        ModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(parameterResolvers);
        parameterResolvers.register(new ModuleInstanceFactoryParameterResolver(instanceFactory));
        return instanceFactory;
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

        LOGGER.info("Module register completed. Success: {} | Issues: {} | Duration: {}ms",
                registered.passed().size(),
                issues.size(),
                System.currentTimeMillis() - startTime
        );
        return new ModuleRegistrationResult(registered.passed(), issues);
    }

    private ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> modules = new ArrayList<>();
        for (ModuleProvider moduleProvider : moduleProviders) {
            try {
                modules.addAll(moduleProvider.discoverModules());
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
        return ModuleRegistrationStageResult.of(modules);
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
        List<ModuleCandidate> availableModules = new ArrayList<>();
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        for (ModuleCandidate module : modules) {
            if (conflicts.contains(module.id())) {
                String reason = "Duplicate module id: " + module.id();
                logSkip(module, reason);
                issues.add(new DuplicateModuleIdIssue(module, reason));
            } else {
                availableModules.add(module);
            }
        }
        return ModuleRegistrationStageResult.of(availableModules, new ModuleRegistrationIssues(issues));
    }

    private ModuleRegistrationStageResult<ModuleContext> registerResolvedModules(List<ModuleCandidate> modules) {
        List<ModuleContext> contexts = new ArrayList<>();
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        for (ModuleCandidate module : modules) {
            try {
                eventBus.publish(new ModuleRegisterEvent.Pre(module));
                Module instance = instanceFactory.create(module);
                ModuleContext context = new ModuleContext(instance, module);
                eventBus.publish(new ModuleRegisterEvent.Post(context));
                contexts.add(context);
                logRegisterSuccess(module);
            } catch (ModuleInstantiationException e) {
                String reason = "Failed to instantiate module: " + e.getMessage();
                logSkip(module, reason);
                issues.add(new InstantiationFailedIssue(module, reason, e));
            } catch (RuntimeException e) {
                throw new ModuleRegistrationException(
                        "Failed to register module: " + module.id(),
                        e
                );
            }
        }
        return ModuleRegistrationStageResult.of(contexts, new ModuleRegistrationIssues(issues));
    }

    private Set<String> findConflictIds(List<ModuleCandidate> modules) {
        Map<String, Long> countById = modules.stream()
                .collect(Collectors.groupingBy(module -> module.id(), Collectors.counting()));
        return countById.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void logRegisterSuccess(ModuleCandidate module) {
        LOGGER.info("Successfully register module: {} ({}) - {}",
                module.name(), module.id(), module.description()
        );
    }

    private void logSkip(ModuleCandidate module, String reason) {
        LOGGER.warn(
                "Skipping module: {} ({}) - {}",
                module.name(),
                module.id(),
                reason
        );
    }
}
