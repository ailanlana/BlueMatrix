package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.BlueMatrixContainerException;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionLoader;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleResolverComposition;
import io.fntlv.bluematrix.core.module.orchestration.DefaultModuleOrchestrator;
import io.fntlv.bluematrix.core.module.orchestration.ModuleOrchestrator;
import io.fntlv.bluematrix.core.module.registration.DefaultModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;

public final class BlueMatrixBootstrap {
    public BlueMatrixBootstrapResult build(BlueMatrixBootstrapPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan cannot be null");
        }
        BlueMatrixExtensionLoader extensionLoader = new BlueMatrixExtensionLoader(plan.classLoader()).load();
        loadCoreLibraries(plan);
        extensionLoader.apply(new DefaultBlueMatrixExtensionBootstrap(plan));
        loadConfiguredLibraries(plan);
        if (plan.moduleProviders().isEmpty()) {
            throw new BlueMatrixContainerException("At least one module provider is required");
        }
        return new BlueMatrixBootstrapResult(assembleRuntime(plan), extensionLoader);
    }

    private void loadCoreLibraries(BlueMatrixBootstrapPlan plan) {
        try {
            plan.libraryLoader().loadCoreLibraries();
        } catch (RuntimeException e) {
            throw new BlueMatrixContainerException("Failed to load BlueMatrix runtime libraries", e);
        }
    }

    private void loadConfiguredLibraries(BlueMatrixBootstrapPlan plan) {
        try {
            plan.libraryLoader().loadAppLibraries();
            plan.libraryLoader().loadExtensionLibraries();
        } catch (RuntimeException e) {
            throw new BlueMatrixContainerException("Failed to load BlueMatrix runtime libraries", e);
        }
    }

    private BlueMatrixContainerRuntime assembleRuntime(BlueMatrixBootstrapPlan plan) {
        ModuleStore moduleStore = new ModuleStore();
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        ModuleRegistry registry = new DefaultModuleRegistry(moduleStore, plan.dataFolder());
        ModuleResolverComposition resolverComposition = ModuleResolverComposition.forContainer(
                registry,
                eventBus,
                plan.parameterResolvers()
        );
        ModuleParameterResolverRegistry parameterResolvers = resolverComposition.resolvers();
        ModuleInstanceFactory instanceFactory = resolverComposition.instanceFactory();
        ModuleRegistrar moduleRegistrar = new DefaultModuleRegistrar(
                plan.moduleProviders(),
                new TopologyDependencyResolver(),
                eventBus,
                instanceFactory
        );
        ModuleOrchestrator moduleOrchestrator = new DefaultModuleOrchestrator(moduleStore, moduleRegistrar, eventBus);
        registerListeners(eventBus, plan);
        moduleOrchestrator.initialize();
        return new BlueMatrixContainerRuntime(
                registry,
                moduleOrchestrator,
                eventBus,
                parameterResolvers,
                instanceFactory
        );
    }

    private void registerListeners(ModuleEventBus eventBus, BlueMatrixBootstrapPlan plan) {
        for (Object eventListener : plan.eventListeners()) {
            eventBus.registerListener(eventListener);
        }
    }
}
