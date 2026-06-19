package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleRegistrationException;
import io.fntlv.bluematrix.core.module.registration.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.resolver.ModuleEventBusParameterResolver;
import io.fntlv.bluematrix.core.module.registration.resolver.DependencyResolver;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.ModuleContext;

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
    private final ModuleParameterResolverRegistry parameterResolvers;

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
        this(moduleProviders, dependencyResolver, eventBus, instanceFactory, createDefaultParameterResolvers(eventBus));
    }

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders,
                                  DependencyResolver dependencyResolver,
                                  ModuleEventBus eventBus,
                                  ModuleParameterResolverRegistry parameterResolvers) {
        this(moduleProviders, dependencyResolver, eventBus, new DefaultModuleInstanceFactory(parameterResolvers), parameterResolvers);
    }

    public DefaultModuleRegistrar(List<ModuleProvider> moduleProviders,
                                  DependencyResolver dependencyResolver,
                                  ModuleEventBus eventBus,
                                  ModuleInstanceFactory instanceFactory,
                                  ModuleParameterResolverRegistry parameterResolvers) {
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
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        this.moduleProviders = Collections.unmodifiableList(new ArrayList<>(moduleProviders));
        this.dependencyResolver = dependencyResolver;
        this.eventBus = eventBus;
        this.instanceFactory = instanceFactory;
        this.parameterResolvers = parameterResolvers;
    }

    private static ModuleParameterResolverRegistry createDefaultParameterResolvers(ModuleEventBus eventBus) {
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        parameterResolvers.register(new ModuleEventBusParameterResolver(eventBus));
        return parameterResolvers;
    }

    @Override
    public List<ModuleContext> register() {
        final long startTime = System.currentTimeMillis();
        LOGGER.infoBanner();

        List<ModuleCandidate> modules = discoverModules();

        List<ModuleCandidate> availableModules = removeIdConflicts(modules);
        List<ModuleCandidate> loadOrder = resolveDependencies(availableModules);
        List<ModuleContext> contexts = registerResolvedModules(loadOrder);

        LOGGER.info("Module register completed. Success: {} | Duration: {}ms",
                contexts.size(),
                System.currentTimeMillis() - startTime
        );
        return contexts;
    }

    private List<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> modules = new ArrayList<>();
        for (ModuleProvider moduleProvider : moduleProviders) {
            try {
                modules.addAll(moduleProvider.discoverModules());
            } catch (ModuleDiscoveryException e) {
                LOGGER.warn("Skipping module provider: {} - {}",
                        moduleProvider.getClass().getName(),
                        e.getMessage());
            } catch (RuntimeException e) {
                throw new ModuleRegistrationException(
                        "Failed to discover modules from provider: " + moduleProvider.getClass().getName(),
                        e
                );
            }
        }
        return modules;
    }

    private List<ModuleCandidate> resolveDependencies(List<ModuleCandidate> modules) {
        try {
            return dependencyResolver.resolve(modules);
        } catch (RuntimeException e) {
            throw new ModuleRegistrationException("Failed to resolve module dependencies", e);
        }
    }

    private List<ModuleCandidate> removeIdConflicts(List<ModuleCandidate> modules) {
        Set<String> conflicts = findConflictIds(modules);
        List<ModuleCandidate> availableModules = new ArrayList<>();
        for (ModuleCandidate module : modules) {
            if (conflicts.contains(module.getModuleInfo().id())) {
                logSkip(module, "Duplicate module id: " + module.getModuleInfo().id());
            } else {
                availableModules.add(module);
            }
        }
        return availableModules;
    }

    private List<ModuleContext> registerResolvedModules(List<ModuleCandidate> modules) {
        List<ModuleContext> contexts = new ArrayList<>();
        for (ModuleCandidate module : modules) {
            try {
                eventBus.publish(new ModuleRegisterEvent.Pre(module));
                Module instance = instanceFactory.create(module);
                ModuleContext context = new ModuleContext(instance, module);
                eventBus.publish(new ModuleRegisterEvent.Post(context));
                contexts.add(context);
                logRegisterSuccess(module);
            } catch (ModuleInstantiationException e) {
                logSkip(module, "Failed to instantiate module: " + e.getMessage());
            } catch (RuntimeException e) {
                throw new ModuleRegistrationException(
                        "Failed to register module: " + module.getModuleInfo().id(),
                        e
                );
            }
        }
        return contexts;
    }

    private Set<String> findConflictIds(List<ModuleCandidate> modules) {
        Map<String, Long> countById = modules.stream()
                .collect(Collectors.groupingBy(module -> module.getModuleInfo().id(), Collectors.counting()));
        return countById.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void logRegisterSuccess(ModuleCandidate module) {
        ModuleInfo info = module.getModuleInfo();
        LOGGER.info("Successfully register module: {} ({}) - {}",
                info.name(), info.id(), info.description()
        );
    }

    private void logSkip(ModuleCandidate module, String reason) {
        ModuleInfo info = module.getModuleInfo();
        LOGGER.warn(
                "Skipping module: {} ({}) - {}",
                info.name(),
                info.id(),
                reason
        );
    }
}
